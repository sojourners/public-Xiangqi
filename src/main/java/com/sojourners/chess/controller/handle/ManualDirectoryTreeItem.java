package com.sojourners.chess.controller.handle;

import com.sojourners.chess.util.PathUtils;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 目录只在首次展开时后台读取一层；树节点的修改均在 JavaFX 线程执行。 */
final class ManualDirectoryTreeItem extends TreeItem<File> {

    private static final Comparator<TreeItem<File>> ITEM_ORDER = Comparator
            .comparing((TreeItem<File> item) -> !(item instanceof ManualDirectoryTreeItem))
            .thenComparing(item -> item.getValue().getName(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(item -> item.getValue().getName());

    private final Set<String> extensions;
    private final Set<File> pendingSavedFiles = new LinkedHashSet<>();
    private boolean loaded;
    private boolean loading;
    private long loadVersion;

    ManualDirectoryTreeItem(File directory, Set<String> extensions) {
        super(directory);
        this.extensions = Set.copyOf(extensions);
        expandedProperty().addListener((obs, old, expanded) -> {
            if (expanded) loadChildren();
        });
        valueProperty().addListener((obs, old, value) -> {
            // 重命名后旧子节点路径及在途扫描结果都已失效。
            loadVersion++;
            loaded = false;
            loading = false;
            pendingSavedFiles.clear();
            getChildren().clear();
            if (isExpanded()) loadChildren();
        });
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    private void loadChildren() {
        if (loaded || loading || getValue() == null) return;
        loading = true;
        long version = ++loadVersion;
        File directory = getValue();
        Thread.startVirtualThread(() -> {
            List<Entry> entries = readDirectory(directory);
            Platform.runLater(() -> {
                if (version != loadVersion) return;
                List<TreeItem<File>> children = new ArrayList<>(entries.size());
                for (Entry entry : entries) {
                    children.add(newChild(entry.file(), entry.directory()));
                }
                loading = false;
                loaded = true;
                getChildren().setAll(children);
                // 扫描快照可能早于保存完成，补入期间新增的棋谱。
                List<File> savedFiles = new ArrayList<>(pendingSavedFiles);
                pendingSavedFiles.clear();
                savedFiles.forEach(this::savedFile);
            });
        });
    }

    private List<Entry> readDirectory(File directory) {
        List<Entry> entries = new ArrayList<>();
        try {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    boolean isDirectory = file.isDirectory();
                    if (isDirectory || (isSupported(file) && file.isFile())) {
                        entries.add(new Entry(file, isDirectory));
                    }
                }
                // 枚举时记录类型，排序不再访问文件系统。
                entries.sort(Comparator.comparing(Entry::directory).reversed()
                        .thenComparing(entry -> entry.file().getName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(entry -> entry.file().getName()));
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return entries;
    }

    /** 保存只更新已加载的路径，未展开的目录留待首次展开时读取。 */
    void savedFile(File file) {
        if (getValue() == null || !isSupported(file)) return;
        Path directory = getValue().toPath().toAbsolutePath().normalize();
        Path savedPath = file.toPath().toAbsolutePath().normalize();
        if (savedPath.equals(directory) || !savedPath.startsWith(directory)) return;
        if (loading) {
            pendingSavedFiles.add(file);
            return;
        }
        if (!loaded) return;

        Path relative = directory.relativize(savedPath);
        Path childPath = directory.resolve(relative.getName(0));
        TreeItem<File> child = newChild(relative.getNameCount() == 1 ? file : childPath.toFile(),
                relative.getNameCount() > 1);
        int index = Collections.binarySearch(getChildren(), child, ITEM_ORDER);
        if (index < 0) {
            getChildren().add(-index - 1, child);
        } else {
            child = getChildren().get(index);
        }
        if (relative.getNameCount() > 1 && child instanceof ManualDirectoryTreeItem item) {
            item.savedFile(file);
        }
    }

    private TreeItem<File> newChild(File file, boolean directory) {
        TreeItem<File> child = directory
                ? new ManualDirectoryTreeItem(file, extensions) : new TreeItem<>(file);
        child.valueProperty().addListener((obs, old, value) -> {
            // 重命名也要维持顺序，供后续保存时二分查找和插入。
            if (child.getParent() == this) getChildren().sort(ITEM_ORDER);
        });
        return child;
    }

    private boolean isSupported(File file) {
        return extensions.contains(PathUtils.getDotExtension(file).toLowerCase(Locale.ROOT));
    }

    private record Entry(File file, boolean directory) { }
}

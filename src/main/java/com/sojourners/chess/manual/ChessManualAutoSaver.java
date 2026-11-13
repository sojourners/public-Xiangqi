package com.sojourners.chess.manual;

import com.sojourners.chess.controller.handle.ChessManualHandle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** 保存新建或重置局面前的棋谱备份。 */
public final class ChessManualAutoSaver {

    private static final DateTimeFormatter FILE_NAME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    private ChessManualAutoSaver() {
    }

    public static File save(ChessManual manual, String autoSaveDirectory, String format) throws IOException {
        return save(manual, autoSaveDirectory, format, Clock.systemDefaultZone());
    }

    static File save(ChessManual manual, String autoSaveDirectory, String format, Clock clock)
            throws IOException {
        String extension = format.toLowerCase(Locale.ROOT);
        ChessManualService service = ChessManualHandle.manualServices.get(extension);

        Path directory = Files.createDirectories(Path.of(autoSaveDirectory));

        File file = directory.resolve(LocalDateTime.now(clock).format(FILE_NAME_FORMAT) + "." + extension).toFile();
        service.saveChessManual(manual, file);
        return file;
    }
}

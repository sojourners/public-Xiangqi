package com.sojourners.chess.openbook;

import com.sojourners.chess.board.ChessBoard;
import com.sojourners.chess.config.Properties;
import com.sojourners.chess.model.BookData;

import java.util.ArrayList;
import java.util.List;

public class OpenBookManager {

    private volatile static OpenBookManager instance;

    private OpenBook cloudOpenBook;
    private List<OpenBook> localOpenBooks;
    Properties prop;

    private OpenBookManager() {
        this.cloudOpenBook = new CloudOpenBook();
        this.localOpenBooks = new ArrayList<>();
        prop = Properties.getInstance();

        setLocalOpenBooks();
    }

    public synchronized void close() {
        for (OpenBook ob : localOpenBooks) {
            ob.close();
        }
    }

    public synchronized void setLocalOpenBooks() {
        close();
        localOpenBooks.clear();
        for (String path : prop.getOpenBookList()) {
            try {
                if (path.endsWith(".obk")) {
                    localOpenBooks.add(new BhOpenBook(path));
                } else if (path.endsWith(".pfBook")) {
                    localOpenBooks.add(new PfOpenBook(path));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized List<BookData> queryBook(char[][] b, boolean redGo, boolean offManual) {

        List<BookData> cloudResults = new ArrayList<>();
        if (prop.getUseCloudBook()) {
            String fenCode = ChessBoard.fenCode(b, redGo);
            cloudResults.addAll(cloudOpenBook.query(fenCode, offManual, prop.getMoveRule()));
        }

        List<BookData> localResults = new ArrayList<>();
        if (!offManual) {
            for (OpenBook ob : this.localOpenBooks) {
                localResults.addAll(ob.query(b, redGo, prop.getMoveRule()));
            }
        }

        if (prop.getLocalBookFirst()) {
            localResults.addAll(cloudResults);
            return localResults;
        } else {
            cloudResults.addAll(localResults);
            return cloudResults;
        }
    }

    public static OpenBookManager getInstance() {
        if (instance == null) {
            synchronized (OpenBookManager.class) {
                if (instance == null) {
                    instance = new OpenBookManager();
                }
            }
        }
        return instance;
    }



}

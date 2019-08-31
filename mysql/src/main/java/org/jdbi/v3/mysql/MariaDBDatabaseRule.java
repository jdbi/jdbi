package org.jdbi.v3.mysql;

import javax.sql.DataSource;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.jdbi.v3.core.internal.exceptions.Sneaky;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.testing.JdbiRule;

public class MariaDBDatabaseRule extends JdbiRule {
    private DB db;

    @Override
    protected DataSource createDataSource() {
        String dbName = "test";
        try {
            db = DB.newEmbeddedDB(DBConfigurationBuilder.newBuilder()
                    .build());
            db.start();
            db.createDB(dbName);
            MysqlDataSource ds = new MysqlDataSource();
            String url = db.getConfiguration().getURL(dbName);
            ds.setUrl(url + (url.contains("?") ? "&" : "?") + "serverTimezone=UTC");
            return ds;
        } catch (ManagedProcessException e) {
            throw Sneaky.throwAnyway(e);
        }
    }

    @Override
    public void after() {
        super.after();
        if (db != null) {
            Unchecked.runnable(db::stop).run();
        }
    }

}

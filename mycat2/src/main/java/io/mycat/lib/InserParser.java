package io.mycat.lib;


import com.alibaba.fastsql.DbType;
import com.alibaba.fastsql.sql.SQLUtils;
import com.alibaba.fastsql.sql.ast.SQLStatement;
import com.alibaba.fastsql.sql.ast.expr.SQLCharExpr;
import com.alibaba.fastsql.sql.ast.expr.SQLValuableExpr;
import com.alibaba.fastsql.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import com.alibaba.fastsql.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.fastsql.sql.dialect.mysql.parser.MySqlStatementParser;
import io.mycat.api.collector.AbstractObjectRowIterator;
import io.mycat.api.collector.AbstractStringRowIterator;
import io.mycat.beans.mycat.MycatRowMetaData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import static com.alibaba.fastsql.sql.parser.SQLParserFeature.*;

public class InserParser extends AbstractStringRowIterator {
    public InserParser(String path) throws Exception {
        this(Files.lines(Paths.get(InserParser.class.getResource(path).toURI())).iterator());
    }

    public InserParser(Iterator<String> source) {
        this(getCreateTableSQL(source), source);
    }

    protected static String getCreateTableSQL(Iterator<String> lines) {
        StringBuilder createTableStmtBuffer = new StringBuilder();
        String lastLine = null;
        while (lines.hasNext()) {
            lastLine = lines.next().trim();
            createTableStmtBuffer.append(lastLine);
            if (!lastLine.contains("insert") && lastLine.contains(");")) {
                break;
            }
        }
        return createTableStmtBuffer.toString();
    }

    final static String[] EMPTY = new String[]{};

    public InserParser(String createTableStmttext, Iterator<String> lines) {
        super(getMycatRowMetaData(createTableStmttext), StreamSupport.stream(Spliterators.spliteratorUnknownSize(lines, 0), false).flatMap(s -> {
            MySqlStatementParser sqlStatementParser = new MySqlStatementParser(s, UseInsertColumnsCache, InsertValueNative, OptimizedForParameterized, OptimizedForForParameterizedSkipValue, InsertValueCheckType);
            MySqlInsertStatement insertStatement = (MySqlInsertStatement) sqlStatementParser.parseInsert();
            return insertStatement.getValuesList().stream();
        }).map(valuesClause -> {
            List valuesClause1 = valuesClause.getValues();
            if (valuesClause1.isEmpty()) {
                return EMPTY;
            } else {
                Object o = valuesClause1.get(0);
                if (o instanceof String) {
                    return (String[]) valuesClause1.toArray(EMPTY);
                } else {
                    String[] objects = new String[valuesClause.getValues().size()];
                    int i = 0;
                    for (Object value : valuesClause.getValues()) {
                        if (value instanceof SQLValuableExpr) {
                            objects[i++] = ((SQLCharExpr) value).getText();
                        } else if (value instanceof String) {
                            throw new UnsupportedOperationException();
                        }
                    }
                    return objects;
                }
            }
        }).iterator());
    }

    private static MycatRowMetaDataImpl getMycatRowMetaData(String createTableStmttext) {
        List<SQLStatement> statements = SQLUtils.parseStatements(createTableStmttext, DbType.mysql);
        MySqlCreateTableStatement mySqlCreateTableStatement = (MySqlCreateTableStatement) statements.get(statements.size() - 1);
        String tableName = mySqlCreateTableStatement.getTableSource().computeAlias();
        return new MycatRowMetaDataImpl(mySqlCreateTableStatement.getColumnDefinitions(), "", tableName);
    }

    @Override
    public void close() {

    }

    public static void main(String[] args) throws IOException {
        Iterator<String> iterator = Files.lines(Paths.get("d:/show_databases.sql")).iterator();
        InserParser inserParser = new InserParser(iterator);
        MycatRowMetaData rowMetaData = inserParser.metaData();
        System.out.println(rowMetaData);
        int columnCount = rowMetaData.getColumnCount();
        Object[] row = new Object[columnCount];
        while (inserParser.next()) {
            for (int i = 0; i < columnCount; i++) {
                row[i] = inserParser.getString(i);
            }
            System.out.println(Arrays.toString(row));
        }

        System.out.println();


    }
}
package com.example.orm.querybuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class QueryBuilder {
    private String table;
    private List<String> columns;
    private List<String> whereClauses;
    private List<String> groupByColumns;
    private List<String> havingClauses;

    public QueryBuilder() {
        this.columns = new ArrayList<>();
        this.whereClauses = new ArrayList<>();
        this.groupByColumns = new ArrayList<>();
        this.havingClauses = new ArrayList<>();
    }

    public QueryBuilder select(String... columns) {
        for (String column : columns) {
            this.columns.add(column);
        }
        return this;
    }

    public QueryBuilder from(String table) {
        this.table = table;
        return this;
    }

    public QueryBuilder where(String condition) {
        this.whereClauses.add(condition);
        return this;
    }

    public QueryBuilder groupBy(String... columns) {
        for (String column : columns) {
            this.groupByColumns.add(column);
        }
        return this;
    }

    public QueryBuilder having(String condition) {
        this.havingClauses.add(condition);
        return this;
    }

    public String build() {
        if (table == null || columns.isEmpty()) {
            throw new IllegalStateException("Table and columns must be specified");
        }

        StringJoiner query = new StringJoiner(" ");
        query.add("SELECT");
        query.add(String.join(", ", columns));
        query.add("FROM");
        query.add(table);

        if (!whereClauses.isEmpty()) {
            query.add("WHERE");
            query.add(String.join(" AND ", whereClauses));
        }

        if (!groupByColumns.isEmpty()) {
            query.add("GROUP BY");
            query.add(String.join(", ", groupByColumns));
        }

        if (!havingClauses.isEmpty()) {
            query.add("HAVING");
            query.add(String.join(" AND ", havingClauses));
        }

        return query.toString();
    }
}

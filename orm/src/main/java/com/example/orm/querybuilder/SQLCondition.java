package com.example.orm.querybuilder;

import java.util.ArrayList;
import java.util.List;

import com.example.orm.annotations.ColumnAttribute;

public class SQLCondition {
    private final String function;
    private final List<String> columns;
    private String operator;
    private String value;
    
    public enum AggregateFunction {
        SUM("SUM"),
        COUNT("COUNT"),
        AVG("AVG"),
        MIN("MIN"),
        MAX("MAX");
    
        private final String functionName;
    
        AggregateFunction(String functionName) {
            this.functionName = functionName;
        }
    
        public String getFunctionName() {
            return functionName;
        }
    }

    // COUNT, SUM, AVG, MIN, MAX...
    public SQLCondition(AggregateFunction function, ColumnAttribute column) {
        this.function = function.getFunctionName();
        this.columns = new ArrayList<>();
        this.columns.add(column.getColumnName());
    }

    // CONCAT
    public SQLCondition(String function, List<ColumnAttribute> columns) {
        this.function = function;
        this.columns = new ArrayList<>();
        for (ColumnAttribute column : columns) {
            this.columns.add(column.getColumnName());
        }
    }
    
    // COUNT(id) > 1
    public SQLCondition withOperator(String operator, String value) {
        this.operator = operator;
        this.value = value;
        return this;
    }

    // How to use:
    // new SQLCondition("COUNT", "id", "total").withOperator(">", "100")
    // new SQLCondition("CONCAT", List.of("first_name", "last_name"), "full_name")
    @Override
    public String toString() {
        StringBuilder condition = new StringBuilder();
        condition.append(function).append("(");
        condition.append(String.join(", ", columns));
        condition.append(")");

        if (operator != null && value != null) {
            condition.append(" ").append(operator).append(" ").append(value);
        }

        return condition.toString();
    }
}
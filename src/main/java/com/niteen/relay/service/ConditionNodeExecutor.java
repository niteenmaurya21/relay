package com.niteen.relay.service;


import org.springframework.stereotype.Service;

@Service
public class ConditionNodeExecutor {

    public boolean evaluate(String left, String op, String right ) {

        if(left == null || op == null || right == null) {
            throw new IllegalArgumentException(

                    "Condition left, op and right are required"
            );
        }
        return switch (op){
            case "equals" -> left.equals(right);
            case "not_equals" -> !left.equals(right);
            case "contains" -> left.contains(right);
            case "greater_than" -> compareNumbers(left, right) >0 ;
            case "less_than" -> compareNumbers(left, right) <0 ;

            default -> throw new IllegalArgumentException(
                    "Unsuported condition operator" +op
            );
        };

        }
    private int compareNumbers(String left , String right) {
        try {
            double leftNumeber = Double.parseDouble(left);
            double rightNumeber = Double.parseDouble(right);

            return Double.compare(leftNumeber, rightNumeber);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "greater_than/less_than require neumeric values "
            );
        }
    }

}

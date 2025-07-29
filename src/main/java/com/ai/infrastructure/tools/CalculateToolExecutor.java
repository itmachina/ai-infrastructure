package com.ai.infrastructure.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 计算工具执行器
 * 支持基本数学运算：加减乘除、比大小等
 */
public class CalculateToolExecutor implements ToolExecutor {
    @Override
    public String execute(String task) {
        try {
            // 移除前缀"Calculate"或"计算"
            String expression = task.replaceFirst("^(Calculate|calculate|计算)\\s*", "").trim();
            
            // 处理不同的计算类型
            if (isComparisonExpression(expression)) {
                return handleComparison(expression);
            } else if (isArithmeticExpression(expression)) {
                return handleArithmetic(expression);
            } else {
                return "Error: Unsupported calculation type - " + expression;
            }
        } catch (Exception e) {
            return "Error: Failed to parse calculation - " + e.getMessage();
        }
    }
    
    /**
     * 判断是否为比较表达式
     * @param expression 表达式
     * @return 是否为比较表达式
     */
    private boolean isComparisonExpression(String expression) {
        return expression.matches(".*[<>]=?\\s*.*|.*==\\s*.*|.*!=\\s*.*");
    }
    
    /**
     * 判断是否为算术表达式
     * @param expression 表达式
     * @return 是否为算术表达式
     */
    private boolean isArithmeticExpression(String expression) {
        return expression.matches(".*[+\\-*/].*");
    }
    
    /**
     * 处理比较表达式
     * @param expression 表达式
     * @return 比较结果
     */
    private String handleComparison(String expression) {
        // 支持的比较操作符: >, <, >=, <=, ==, !=
        Pattern pattern = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s*(>|<|>=|<=|==|!=)\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(expression);
        
        if (matcher.matches()) {
            double left = Double.parseDouble(matcher.group(1));
            String operator = matcher.group(2);
            double right = Double.parseDouble(matcher.group(3));
            
            boolean result;
            switch (operator) {
                case ">":
                    result = left > right;
                    break;
                case "<":
                    result = left < right;
                    break;
                case ">=":
                    result = left >= right;
                    break;
                case "<=":
                    result = left <= right;
                    break;
                case "==":
                    result = left == right;
                    break;
                case "!=":
                    result = left != right;
                    break;
                default:
                    return "Error: Unsupported comparison operator - " + operator;
            }
            
            return String.format("%s %s %s = %s", matcher.group(1), operator, matcher.group(3), result);
        }
        
        return "Error: Invalid comparison expression - " + expression;
    }
    
    /**
     * 处理算术表达式
     * @param expression 表达式
     * @return 计算结果
     */
    private String handleArithmetic(String expression) {
        try {
            // 移除空格
            expression = expression.replaceAll("\\s+", "");
            
            // 处理简单的二元运算
            Pattern pattern = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s*([+\\-*/])\\s*(-?\\d+(?:\\.\\d+)?)");
            Matcher matcher = pattern.matcher(expression);
            
            if (matcher.matches()) {
                double left = Double.parseDouble(matcher.group(1));
                String operator = matcher.group(2);
                double right = Double.parseDouble(matcher.group(3));
                
                double result;
                switch (operator) {
                    case "+":
                        result = left + right;
                        break;
                    case "-":
                        result = left - right;
                        break;
                    case "*":
                        result = left * right;
                        break;
                    case "/":
                        if (right == 0) {
                            return "Error: Division by zero";
                        }
                        result = left / right;
                        break;
                    default:
                        return "Error: Unsupported arithmetic operator - " + operator;
                }
                
                // 格式化结果，如果是整数则不显示小数点
                if (result == (long) result) {
                    return String.format("%s %s %s = %d", matcher.group(1), operator, matcher.group(3), (long) result);
                } else {
                    return String.format("%s %s %s = %.6f", matcher.group(1), operator, matcher.group(3), result);
                }
            }
            
            // 如果简单模式不匹配，尝试处理更复杂的表达式
            return evaluateComplexExpression(expression);
        } catch (NumberFormatException e) {
            return "Error: Invalid number format in expression - " + expression;
        } catch (Exception e) {
            return "Error: Failed to evaluate arithmetic expression - " + e.getMessage();
        }
    }
    
    /**
     * 计算复杂表达式（简化版本）
     * @param expression 表达式
     * @return 计算结果
     */
    private String evaluateComplexExpression(String expression) {
        try {
            // 这里可以实现更复杂的表达式解析
            // 目前只支持简单的二元运算
            
            // 尝试不同的分割方式
            String[] operators = {"+", "-", "*", "/"};
            for (String op : operators) {
                int index = expression.indexOf(op, 1); // 从位置1开始查找，避免负号
                if (index > 0) {
                    String leftStr = expression.substring(0, index);
                    String rightStr = expression.substring(index + 1);
                    
                    // 处理负号
                    if (rightStr.startsWith("-") && op.equals("-")) {
                        rightStr = expression.substring(index + 1);
                        // 重新查找运算符
                        continue;
                    }
                    
                    try {
                        double left = Double.parseDouble(leftStr);
                        double right = Double.parseDouble(rightStr);
                        
                        double result;
                        switch (op) {
                            case "+":
                                result = left + right;
                                break;
                            case "-":
                                result = left - right;
                                break;
                            case "*":
                                result = left * right;
                                break;
                            case "/":
                                if (right == 0) {
                                    return "Error: Division by zero";
                                }
                                result = left / right;
                                break;
                            default:
                                continue; // 尝试下一个运算符
                        }
                        
                        // 格式化结果
                        if (result == (long) result) {
                            return String.format("%s = %d", expression, (long) result);
                        } else {
                            return String.format("%s = %.6f", expression, result);
                        }
                    } catch (NumberFormatException e) {
                        // 继续尝试下一个运算符
                        continue;
                    }
                }
            }
            
            return "Error: Unsupported complex expression - " + expression;
        } catch (Exception e) {
            return "Error: Failed to evaluate complex expression - " + e.getMessage();
        }
    }
}
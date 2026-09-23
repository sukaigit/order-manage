package com.example.ordermanage.aspect;

import com.example.ordermanage.annotation.OpLog;
import com.example.ordermanage.service.OperationLogWriter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(0)
@ConditionalOnProperty(name = "order.oplog.enabled", havingValue = "true", matchIfMissing = true)
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final OperationLogWriter operationLogWriter;
    private final ExpressionParser parser = new SpelExpressionParser();

    public OperationLogAspect(OperationLogWriter operationLogWriter) {
        this.operationLogWriter = operationLogWriter;
    }

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint pjp, OpLog opLog) throws Throwable {
        Object result = pjp.proceed();
        try {
            String target = resolveTarget(opLog.targetSpEL(), pjp, result);
            String operator = OperationLogWriter.currentOperator();
            if (operator == null && target != null && "登录".equals(opLog.action())) {
                operator = target;
            }
            operationLogWriter.write(operator, opLog.action(), target);
        } catch (Exception e) {
            log.warn("operation log write failed: {}", e.getMessage());
        }
        return result;
    }

    private String resolveTarget(String expression, ProceedingJoinPoint pjp, Object result) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        try {
            SpelRoot root = new SpelRoot(pjp.getArgs(), pjp.getTarget(), result);
            StandardEvaluationContext context = new StandardEvaluationContext(root);
            context.setVariable("result", result);
            Object value = parser.parseExpression(expression).getValue(context);
            return value == null ? null : value.toString();
        } catch (Exception e) {
            log.warn("oplog target expression failed: {} ({})", expression, e.getMessage());
            return null;
        }
    }

    public static final class SpelRoot {
        private final Object[] args;
        private final Object target;
        private final Object result;

        public SpelRoot(Object[] args, Object target, Object result) {
            this.args = args;
            this.target = target;
            this.result = result;
        }

        public Object[] getArgs() {
            return args;
        }

        public Object getTarget() {
            return target;
        }

        public Object getResult() {
            return result;
        }
    }
}

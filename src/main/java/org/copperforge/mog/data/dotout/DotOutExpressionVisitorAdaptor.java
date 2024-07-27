package org.copperforge.mog.data.dotout;

import java.util.Stack;

import org.copperforge.mog.data.MogFetchable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gwt.conversion.dotout.DOLong;
import com.gwt.conversion.dotout.DOString;
import com.gwt.conversion.dotout.DOValue;

import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.schema.Column;

public class DotOutExpressionVisitorAdaptor extends ExpressionVisitorAdapter<MogFetchable> {

    private final Logger log = LoggerFactory.getLogger(DotOutExpressionVisitorAdaptor.class);

    private Stack<DOValue<?>> values = new Stack<>();
    private boolean accepted = false;

    public boolean accepted() {
        return accepted;
    }

    @Override
    public <S> MogFetchable visit(EqualsTo expr, S context) {
        log.debug("EqualsTo :: expr = " + expr);
        super.visit(expr, context);

        log.debug("EqualsTo :: left expr = " + expr.getLeftExpression());
        log.debug("EqualsTo :: right expr = " + expr.getRightExpression());

        DOValue<?> right = values.pop();
        DOValue<?> left = values.pop();

        log.debug("EqualsTo :: left value = " + left);
        log.debug("EqualsTo :: right value = " + expr.getRightExpression());

        accepted = (right.value().toString().trim().equals(left.value().toString().trim()));
        return (MogFetchable) context;
    }

    @Override
    public <S> MogFetchable visit(AndExpression expr, S context) {
        log.debug("AndExpression :: expr = " + expr);
        expr.getLeftExpression().accept(this, context);
        boolean leftAccepted = accepted;
        log.debug("AndExpression :: leftAccepted = " + leftAccepted);

        expr.getRightExpression().accept(this, context);
        log.debug("AndExpression :: rightAccepted = " + accepted);
        accepted &= leftAccepted;

        log.debug("AndExpression :: Accepted = " + accepted);
        return (MogFetchable) context;
    }

    @Override
    public <S> MogFetchable visit(OrExpression expr, S context) {
        log.debug("OrExpression :: expr = " + expr);
        expr.getLeftExpression().accept(this, context);
        boolean leftAccepted = accepted;

        expr.getRightExpression().accept(this, context);
        accepted |= leftAccepted;

        return (MogFetchable) context;
    }

    @Override
    public <S> MogFetchable visit(Between expr, S context) {
        log.debug("Between :: expr = " + expr);

        super.visit(expr, context);

        DOValue<?> a = values.pop();
        DOValue<?> b = values.pop();
        DOValue<?> c = values.pop();

        log.debug("a = " + a + ", b = " + b + ", c = " + c);

        return (MogFetchable) context;
    }

    @Override
    public <S> MogFetchable visit(MinorThan expr, S context) {
        log.debug("MinorThan :: expr = " + expr);
        super.visit(expr, context);

        DOValue<?> right = values.pop();
        DOValue<?> left = values.pop();
        log.debug("MinorThan :: left expr = " + left + ", right expr = " + right);

        accepted = (left.compareTo(right) < 0);
        return (MogFetchable) context;
    }

    @Override
    public <S> MogFetchable visit(MinorThanEquals expr, S context) {
        log.debug("MinorThanEquals :: expr = " + expr);
        super.visit(expr, context);

        DOValue<?> right = values.pop();
        DOValue<?> left = values.pop();
        log.debug("MinorThanEquals :: left expr = " + left + ", right expr = " + right);

        accepted = (left.compareTo(right) <= 0);
        log.debug("MinorThanEquals :: Accepted (" + left.value().toString() + " <= " + right.value().toString() + ")= " + accepted);
        return (MogFetchable) context;
    }

    @Override
    public <S> MogFetchable visit(GreaterThan expr, S context) {
        log.debug("GreaterThan :: expr = " + expr);
        super.visit(expr, context);

        DOValue<?> right = values.pop();
        DOValue<?> left = values.pop();
        log.debug("GreaterThan :: left expr = " + left + ", right expr = " + right);

        accepted = (left.compareTo(right) > 0);
        return (MogFetchable) context;
    }

    @Override
    public <S> MogFetchable visit(GreaterThanEquals expr, S context) {
        log.debug("GreaterThanEquals :: expr = " + expr);
        super.visit(expr, context);

        DOValue<?> right = values.pop();
        DOValue<?> left = values.pop();
        log.debug("GreaterThanEquals :: left expr = " + left + ", right expr = " + right);

        accepted = (left.compareTo(right) >= 0);
        log.debug("GreaterThanEquals :: Accepted = " + accepted);
        return (MogFetchable) context;
    }

    @Override
    public <S> MogFetchable visit(NotEqualsTo expr, S context) {
        log.debug("NotEqualsTo :: expr = " + expr);
        super.visit(expr, context);

        DOValue<?> right = values.pop();
        DOValue<?> left = values.pop();
        log.debug("NotEqualsTo :: left expr = " + left + ", right expr = " + right);

        accepted = (left.compareTo(right) != 0);
        log.debug("NotEqualsTo :: Accepted = " + accepted);
        return (MogFetchable) context;
    }

    @Override
    public <S> MogFetchable visit(LongValue expr, S context) {
        log.debug("LongValue :: context = " + context);
        log.debug("LongValue :: expr (" + expr.getClass().getName() + ") = " + expr);

        values.push(new DOLong(expr.getValue()));
        return (MogFetchable) context;
    }

    @Override
    public <S> MogFetchable visit(StringValue expr, S context) {
        log.debug("LongValue :: context = " + context);
        log.debug("LongValue :: expr (" + expr.getClass().getName() + ") = " + expr);

        values.push(new DOString(null, expr.getValue().getBytes()));
        return (MogFetchable) context;
    }

    @Override
    public <S> MogFetchable visit(Column expr, S context) {
        log.debug("Column :: context = " + context);
        log.debug("Column :: expr (" + expr.getClass().getName() + ") = " + expr);

        log.debug("Column :: value = " + ((MogFetchable) context).get(expr.getColumnName()));

        DOValue<?> value = (DOValue<?>) ((MogDotOutFetchable) context).value(expr.getColumnName());
        values.push(value);
        return (MogFetchable) context;
    }

}


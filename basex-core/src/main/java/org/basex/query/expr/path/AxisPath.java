package org.basex.query.expr.path;

import static org.basex.query.expr.path.PathCache.State;
import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.iter.*;
import org.basex.query.util.list.*;
import org.basex.query.value.*;
import org.basex.query.value.node.*;
import org.basex.util.*;

/**
 * Abstract axis path expression.
 *
 * @author BaseX Team 2005-16, BSD License
 * @author Christian Gruen
 */
public abstract class AxisPath extends Path {
  /** Thread-safe path caching. */
  private final ThreadLocal<PathCache> caches = new ThreadLocal<PathCache>() {
    @Override
    public PathCache initialValue() {
      return new PathCache();
    }
  };

  /**
   * Constructor.
   * @param info input info
   * @param root root expression; can be a {@code null} reference
   * @param steps axis steps
   */
  AxisPath(final InputInfo info, final Expr root, final Expr... steps) {
    super(info, root, steps);
  }

  @Override
  public final Iter iter(final QueryContext qc) throws QueryException {
    final PathCache cache = caches.get();
    switch(cache.state) {
      case INIT:
        // first invocation: initialize caching flag
        cache.state = !hasFreeVars() && !has(Flag.NDT) && !has(Flag.UPD)
            ? State.ENABLED : State.DISABLED;
        return iter(qc);
      case ENABLED:
        // second invocation, caching is enabled: cache context value (copy light-weight db nodes)
        final Value value = qc.focus.value;
        cache.initial = value instanceof DBNode ? ((DBNode) value).finish() : value;
        cache.state = State.READY;
        break;
      case READY:
        // third invocation, ready for caching: cache result if context has not changed
        if(cache.sameContext(qc.focus.value, root)) {
          cache.result = nodeIter(qc).value();
          cache.state = State.CACHED;
        } else {
          // disable caching if context has changed
          cache.state = State.DISABLED;
        }
        break;
      case CACHED:
        // further invocations, result is cached: disable caching if context has changed
        if(!cache.sameContext(qc.focus.value, root)) {
          cache.result = null;
          cache.state = State.DISABLED;
        }
        break;
      case DISABLED:
    }

    // iterate or return cached values
    final Value result = cache.result;
    return result == null ? nodeIter(qc) : result.iter();
  }

  /**
   * Returns a node iterator.
   * @param qc query context
   * @return iterator
   * @throws QueryException query exception
   */
  protected abstract NodeIter nodeIter(QueryContext qc) throws QueryException;

  /**
   * Inverts a location path.
   * @param rt new root node
   * @param curr current location step
   * @return inverted path
   */
  public final Path invertPath(final Expr rt, final Step curr) {
    // add predicates of last step to new root node
    int s = steps.length - 1;
    final Expr r = step(s).preds.length == 0 ? rt : Filter.get(info, rt, step(s).preds);

    // add inverted steps in a backward manner
    final ExprList stps = new ExprList();
    while(--s >= 0) {
      stps.add(Step.get(info, step(s + 1).axis.invert(), step(s).test, step(s).preds));
    }
    stps.add(Step.get(info, step(s + 1).axis.invert(), curr.test));
    return Path.get(info, r, stps.finish());
  }

  /**
   * Returns the specified axis step.
   * @param i index
   * @return step
   */
  public final Step step(final int i) {
    return (Step) steps[i];
  }

  @Override
  public final boolean iterable() {
    return true;
  }

  @Override
  public final boolean sameAs(final Expr cmp) {
    if(!(cmp instanceof AxisPath)) return false;
    final AxisPath ap = (AxisPath) cmp;
    if(root == null ? ap.root != null : !root.sameAs(ap.root)) return false;

    final int sl = steps.length;
    if(sl != ap.steps.length) return false;
    for(int s = 0; s < sl; s++) {
      if(!steps[s].sameAs(ap.steps[s])) return false;
    }
    return true;
  }
}

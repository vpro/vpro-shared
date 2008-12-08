/*
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 * Creation date 28 okt 2008.
 */
package nl.vpro.ibatis.rowhandlers;

import com.ibatis.sqlmap.client.event.RowHandler;
import nl.vpro.domain.DomainObject;

import java.util.TreeSet;

/**
 * IBatis RowHandler that returns an embeded result as a {@link TreeSet}.
 * <p/>
 * Ibatis should be able to retrieve results as Sets instead of Lists directly, but I
 * can't get it to work. A workaround is calling a query with a rowhandler. After querying
 * you have to call getSet to retrieve the result as a set from the rowhandler
 *
 * @author roekoe
 */
public class TreeSetRowHandler<DO extends DomainObject> implements RowHandler {
  private TreeSet<DO> set = new TreeSet<DO>();

  @SuppressWarnings("unchecked")
  public void handleRow(Object domainObject) {
    set.add((DO) domainObject);
  }

  public TreeSet<DO> getSet() {
    return set;
  }

}

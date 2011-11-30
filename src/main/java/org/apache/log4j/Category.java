/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

// Contibutors: Alex Blewitt <Alex.Blewitt@ioshq.com>
//              Markus Oestreicher <oes@zurich.ibm.com>
//              Frank Hoering <fhr@zurich.ibm.com>
//              Nelson Minar <nelson@media.mit.edu>
//              Jim Cakalic <jim_cakalic@na.biomerieux.com>
//              Avy Sharell <asharell@club-internet.fr>
//              Ciaran Treanor <ciaran@xelector.com>
//              Jeff Turner <jeff@socialchange.net.au>
//              Michael Horwitz <MHorwitz@siemens.co.za>
//              Calvin Chan <calvin.chan@hic.gov.au>
//              Aaron Greenhouse <aarong@cs.cmu.edu>
//              Beat Meier <bmeier@infovia.com.ar>
//              Colin Sampaleanu <colinml1@exis.com>

package org.apache.log4j;

import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;

import org.apache.log4j.helpers.AppenderAttachableImpl;
import org.apache.log4j.helpers.NullEnumeration;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.HierarchyEventListener;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.LoggingEvent;

public class Category implements AppenderAttachable {

    protected String name;

    volatile protected Level level;

    volatile protected Category parent;

    private static final String FQCN = Category.class.getName();

    protected ResourceBundle resourceBundle;

    protected LoggerRepository repository;

    AppenderAttachableImpl aai;

    protected boolean additive = true;

    protected Category(String name) {
        this.name = name;
    }

    public synchronized void addAppender(Appender newAppender) {
        if (aai == null) {
            aai = new AppenderAttachableImpl();
        }
        aai.addAppender(newAppender);
        repository.fireAddAppenderEvent(this, newAppender);
    }

    public void assertLog(boolean assertion, String msg) {
        if (! assertion) error(msg);
    }

    public void callAppenders(LoggingEvent event) {
        int writes = 0;

        for (Category c = this; c != null; c = c.parent) {
            // Protected against simultaneous call to addAppender, removeAppender,...
            synchronized (c) {
                if (c.aai != null) {
                    writes += c.aai.appendLoopOnAppenders(event);
                }
                if (!c.additive) {
                    break;
                }
            }
        }

        if (writes == 0) {
            repository.emitNoAppenderWarning(this);
        }
    }

    /**
     * Close all attached appenders implementing the AppenderAttachable interface.
     *
     * @since 1.0
     */
    synchronized void closeNestedAppenders() {
        Enumeration enumeration = getAllAppenders();
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                Appender a = (Appender) enumeration.nextElement();
                if (a instanceof AppenderAttachable) {
                    a.close();
                }
            }
        }
    }

    /**
     * Log a message object with the {@link org.apache.log4j.Level#DEBUG DEBUG} level.
     * <p/>
     * <p>This method first checks if this category is <code>DEBUG</code> enabled by comparing the level of this
     * category with the {@link org.apache.log4j.Level#DEBUG DEBUG} level. If this category is <code>DEBUG</code>
     * enabled, then it converts the message object (passed as parameter) to a string by invoking the appropriate {@link
     * org.apache.log4j.or.ObjectRenderer}. It then proceeds to call all the registered appenders in this category and
     * also higher in the hierarchy depending on the value of the additivity flag.
     * <p/>
     * <p><b>WARNING</b> Note that passing a {@link Throwable} to this method will print the name of the
     * <code>Throwable</code> but no stack trace. To print a stack trace use the {@link #debug(Object, Throwable)} form
     * instead.
     *
     * @param message the message object to log.
     */
    public void debug(Object message) {
        if (repository.isDisabled(Level.DEBUG_INT)) return;
        if (Level.DEBUG.isGreaterOrEqual(getEffectiveLevel())) {
            forcedLog(FQCN, Level.DEBUG, message, null);
        }
    }

    /**
     * Log a message object with the <code>DEBUG</code> level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     * <p/>
     * <p>See {@link #debug(Object)} form for more detailed information.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    public void debug(Object message, Throwable t) {
        if (repository.isDisabled(Level.DEBUG_INT)) return;
        if (Level.DEBUG.isGreaterOrEqual(getEffectiveLevel())) forcedLog(FQCN, Level.DEBUG, message, t);
    }

    /**
     * Log a message object with the {@link org.apache.log4j.Level#ERROR ERROR} Level.
     * <p/>
     * <p>This method first checks if this category is <code>ERROR</code> enabled by comparing the level of this
     * category with {@link org.apache.log4j.Level#ERROR ERROR} Level. If this category is <code>ERROR</code> enabled,
     * then it converts the message object passed as parameter to a string by invoking the appropriate {@link
     * org.apache.log4j.or.ObjectRenderer}. It proceeds to call all the registered appenders in this category and also
     * higher in the hierarchy depending on the value of the additivity flag.
     * <p/>
     * <p><b>WARNING</b> Note that passing a {@link Throwable} to this method will print the name of the
     * <code>Throwable</code> but no stack trace. To print a stack trace use the {@link #error(Object, Throwable)} form
     * instead.
     *
     * @param message the message object to log
     */
    public void error(Object message) {
        if (repository.isDisabled(Level.ERROR_INT)) return;
        if (Level.ERROR.isGreaterOrEqual(getEffectiveLevel())) forcedLog(FQCN, Level.ERROR, message, null);
    }

    /**
     * Log a message object with the <code>ERROR</code> level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     * <p/>
     * <p>See {@link #error(Object)} form for more detailed information.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    public void error(Object message, Throwable t) {
        if (repository.isDisabled(Level.ERROR_INT)) return;
        if (Level.ERROR.isGreaterOrEqual(getEffectiveLevel())) forcedLog(FQCN, Level.ERROR, message, t);
    }

    /**
     * If the named category exists (in the default hierarchy) then it returns a reference to the category, otherwise it
     * returns <code>null</code>.
     *
     * @since 0.8.5
     * @deprecated Please use {@link org.apache.log4j.LogManager#exists} instead.
     */
    public
    static Logger exists(String name) {
        return LogManager.exists(name);
    }

    /**
     * Log a message object with the {@link org.apache.log4j.Level#FATAL FATAL} Level.
     * <p/>
     * <p>This method first checks if this category is <code>FATAL</code> enabled by comparing the level of this
     * category with {@link org.apache.log4j.Level#FATAL FATAL} Level. If the category is <code>FATAL</code> enabled,
     * then it converts the message object passed as parameter to a string by invoking the appropriate {@link
     * org.apache.log4j.or.ObjectRenderer}. It proceeds to call all the registered appenders in this category and also
     * higher in the hierarchy depending on the value of the additivity flag.
     * <p/>
     * <p><b>WARNING</b> Note that passing a {@link Throwable} to this method will print the name of the Throwable but
     * no stack trace. To print a stack trace use the {@link #fatal(Object, Throwable)} form instead.
     *
     * @param message the message object to log
     */
    public void fatal(Object message) {
        if (repository.isDisabled(Level.FATAL_INT)) return;
        if (Level.FATAL.isGreaterOrEqual(getEffectiveLevel())) forcedLog(FQCN, Level.FATAL, message, null);
    }

    /**
     * Log a message object with the <code>FATAL</code> level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     * <p/>
     * <p>See {@link #fatal(Object)} for more detailed information.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    public void fatal(Object message, Throwable t) {
        if (repository.isDisabled(Level.FATAL_INT)) return;
        if (Level.FATAL.isGreaterOrEqual(getEffectiveLevel())) forcedLog(FQCN, Level.FATAL, message, t);
    }

    /**
     * This method creates a new logging event and logs the event without further checks.
     */
    protected void forcedLog(String fqcn, Priority level, Object message, Throwable t) {
        callAppenders(new LoggingEvent(fqcn, this, level, message, t));
    }

    /**
     * Get the additivity flag for this Category instance.
     */
    public boolean getAdditivity() {
        return additive;
    }

    /**
     * Get the appenders contained in this category as an {@link java.util.Enumeration}. If no appenders can be found,
     * then a {@link org.apache.log4j.helpers.NullEnumeration} is returned.
     *
     * @return Enumeration An enumeration of the appenders in this category.
     */
    synchronized
    public Enumeration getAllAppenders() {
        if (aai == null) return NullEnumeration.getInstance();
        else return aai.getAllAppenders();
    }

    /**
     * Look for the appender named as <code>name</code>.
     * <p/>
     * <p>Return the appender with that name if in the list. Return <code>null</code> otherwise.
     */
    synchronized
    public Appender getAppender(String name) {
        if (aai == null || name == null) return null;

        return aai.getAppender(name);
    }

    /**
     * Starting from this category, search the category hierarchy for a non-null level and return it. Otherwise, return
     * the level of the root category.
     * <p/>
     * <p>The Category class is designed so that this method executes as quickly as possible.
     */
    public Level getEffectiveLevel() {
        for (Category c = this; c != null; c = c.parent) {
            if (c.level != null) return c.level;
        }
        return null; // If reached will cause an NullPointerException.
    }

    /**
     * @deprecated Please use the the {@link #getEffectiveLevel} method instead.
     */
    public Priority getChainedPriority() {
        for (Category c = this; c != null; c = c.parent) {
            if (c.level != null) return c.level;
        }
        return null; // If reached will cause an NullPointerException.
    }

    /**
     * Returns all the currently defined categories in the default hierarchy as an {@link java.util.Enumeration
     * Enumeration}.
     * <p/>
     * <p>The root category is <em>not</em> included in the returned {@link java.util.Enumeration}.
     *
     * @deprecated Please use {@link org.apache.log4j.LogManager#getCurrentLoggers()} instead.
     */
    public
    static Enumeration getCurrentCategories() {
        return LogManager.getCurrentLoggers();
    }

    /**
     * Return the default Hierarchy instance.
     *
     * @since 1.0
     * @deprecated Please use {@link org.apache.log4j.LogManager#getLoggerRepository()} instead.
     */
    public
    static LoggerRepository getDefaultHierarchy() {
        return LogManager.getLoggerRepository();
    }

    /**
     * Return the the {@link org.apache.log4j.Hierarchy} where this <code>Category</code> instance is attached.
     *
     * @since 1.1
     * @deprecated Please use {@link #getLoggerRepository} instead.
     */
    public LoggerRepository getHierarchy() {
        return repository;
    }

    /**
     * Return the the {@link org.apache.log4j.spi.LoggerRepository} where this <code>Category</code> is attached.
     *
     * @since 1.2
     */
    public LoggerRepository getLoggerRepository() {
        return repository;
    }

    /**
     * @deprecated Make sure to use {@link org.apache.log4j.Logger#getLogger(String)} instead.
     */
    public
    static Category getInstance(String name) {
        return LogManager.getLogger(name);
    }

    /**
     * @deprecated Please make sure to use {@link org.apache.log4j.Logger#getLogger(Class)} instead.
     */
    public
    static Category getInstance(Class clazz) {
        return LogManager.getLogger(clazz);
    }

    /**
     * Return the category name.
     */
    public
    final String getName() {
        return name;
    }

    /**
     * Returns the parent of this category. Note that the parent of a given category may change during the lifetime of
     * the category.
     * <p/>
     * <p>The root category will return <code>null</code>.
     *
     * @since 1.2
     */
    final
    public Category getParent() {
        return parent;
    }

    /**
     * Returns the assigned {@link org.apache.log4j.Level}, if any, for this Category.
     *
     * @return Level - the assigned Level, can be <code>null</code>.
     */
    final
    public Level getLevel() {
        return level;
    }

    /**
     * @deprecated Please use {@link #getLevel} instead.
     */
    final
    public Level getPriority() {
        return level;
    }

    /**
     * @deprecated Please use {@link org.apache.log4j.Logger#getRootLogger()} instead.
     */
    final
    public
    static Category getRoot() {
        return LogManager.getRootLogger();
    }

    /**
     * Return the <em>inherited</em> {@link java.util.ResourceBundle} for this category.
     * <p/>
     * <p>This method walks the hierarchy to find the appropriate resource bundle. It will return the resource bundle
     * attached to the closest ancestor of this category, much like the way priorities are searched. In case there is no
     * bundle in the hierarchy then <code>null</code> is returned.
     *
     * @since 0.9.0
     */
    public ResourceBundle getResourceBundle() {
        for (Category c = this; c != null; c = c.parent) {
            if (c.resourceBundle != null) return c.resourceBundle;
        }
        // It might be the case that there is no resource bundle
        return null;
    }

    /**
     * Returns the string resource coresponding to <code>key</code> in this category's inherited resource bundle. See
     * also {@link #getResourceBundle}.
     * <p/>
     * <p>If the resource cannot be found, then an {@link #error error} message will be logged complaining about the
     * missing resource.
     */
    protected String getResourceBundleString(String key) {
        ResourceBundle rb = getResourceBundle();
        // This is one of the rare cases where we can use logging in order
        // to report errors from within log4j.
        if (rb == null) {
            //if(!hierarchy.emittedNoResourceBundleWarning) {
            //error("No resource bundle has been set for category "+name);
            //hierarchy.emittedNoResourceBundleWarning = true;
            //}
            return null;
        } else {
            try {
                return rb.getString(key);
            } catch (MissingResourceException mre) {
                error("No resource is associated with key \"" + key + "\".");
                return null;
            }
        }
    }

    /**
     * Log a message object with the {@link org.apache.log4j.Level#INFO INFO} Level.
     * <p/>
     * <p>This method first checks if this category is <code>INFO</code> enabled by comparing the level of this category
     * with {@link org.apache.log4j.Level#INFO INFO} Level. If the category is <code>INFO</code> enabled, then it
     * converts the message object passed as parameter to a string by invoking the appropriate {@link
     * org.apache.log4j.or.ObjectRenderer}. It proceeds to call all the registered appenders in this category and also
     * higher in the hierarchy depending on the value of the additivity flag.
     * <p/>
     * <p><b>WARNING</b> Note that passing a {@link Throwable} to this method will print the name of the Throwable but
     * no stack trace. To print a stack trace use the {@link #info(Object, Throwable)} form instead.
     *
     * @param message the message object to log
     */
    public void info(Object message) {
        if (repository.isDisabled(Level.INFO_INT)) return;
        if (Level.INFO.isGreaterOrEqual(getEffectiveLevel())) forcedLog(FQCN, Level.INFO, message, null);
    }

    /**
     * Log a message object with the <code>INFO</code> level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     * <p/>
     * <p>See {@link #info(Object)} for more detailed information.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    public void info(Object message, Throwable t) {
        if (repository.isDisabled(Level.INFO_INT)) return;
        if (Level.INFO.isGreaterOrEqual(getEffectiveLevel())) forcedLog(FQCN, Level.INFO, message, t);
    }

    /**
     * Is the appender passed as parameter attached to this category?
     */
    public boolean isAttached(Appender appender) {
        if (appender == null || aai == null) return false;
        else {
            return aai.isAttached(appender);
        }
    }

    /**
     * Check whether this category is enabled for the <code>DEBUG</code> Level.
     * <p/>
     * <p> This function is intended to lessen the computational cost of disabled log debug statements.
     * <p/>
     * <p> For some <code>cat</code> Category object, when you write,
     * <pre>
     *      cat.debug("This is entry number: " + i );
     *  </pre>
     *
     * <p>You incur the cost constructing the message, concatenatiion in this case, regardless of whether the message is
     * logged or not.
     *
     * <p>If you are worried about speed, then you should write
     * <pre>
     * 	 if(cat.isDebugEnabled()) {
     * 	   cat.debug("This is entry number: " + i );
     *      }
     *  </pre>
     *
     * <p>This way you will not incur the cost of parameter construction if debugging is disabled for <code>cat</code>.
     * On the other hand, if the <code>cat</code> is debug enabled, you will incur the cost of evaluating whether the
     * category is debug enabled twice. Once in <code>isDebugEnabled</code> and once in the <code>debug</code>.  This is
     * an insignificant overhead since evaluating a category takes about 1%% of the time it takes to actually log.
     *
     * @return boolean - <code>true</code> if this category is debug enabled, <code>false</code> otherwise.
     */
    public boolean isDebugEnabled() {
        if (repository.isDisabled(Level.DEBUG_INT)) return false;
        return Level.DEBUG.isGreaterOrEqual(getEffectiveLevel());
    }

    /**
     * Check whether this category is enabled for a given {@link org.apache.log4j.Level} passed as parameter.
     * <p/>
     * See also {@link #isDebugEnabled}.
     *
     * @return boolean True if this category is enabled for <code>level</code>.
     */
    public boolean isEnabledFor(Priority level) {
        if (repository.isDisabled(level.level)) return false;
        return level.isGreaterOrEqual(getEffectiveLevel());
    }

    /**
     * Check whether this category is enabled for the info Level. See also {@link #isDebugEnabled}.
     *
     * @return boolean - <code>true</code> if this category is enabled for level info, <code>false</code> otherwise.
     */
    public boolean isInfoEnabled() {
        if (repository.isDisabled(Level.INFO_INT)) return false;
        return Level.INFO.isGreaterOrEqual(getEffectiveLevel());
    }

    /**
     * Log a localized message. The user supplied parameter <code>key</code> is replaced by its localized version from
     * the resource bundle.
     *
     * @see #setResourceBundle
     * @since 0.8.4
     */
    public void l7dlog(Priority priority, String key, Throwable t) {
        if (repository.isDisabled(priority.level)) {
            return;
        }
        if (priority.isGreaterOrEqual(getEffectiveLevel())) {
            String msg = getResourceBundleString(key);
            // if message corresponding to 'key' could not be found in the
            // resource bundle, then default to 'key'.
            if (msg == null) {
                msg = key;
            }
            forcedLog(FQCN, priority, msg, t);
        }
    }

    /**
     * Log a localized and parameterized message. First, the user supplied <code>key</code> is searched in the resource
     * bundle. Next, the resulting pattern is formatted using {@link java.text.MessageFormat#format(String, Object[])}
     * method with the user supplied object array <code>params</code>.
     *
     * @since 0.8.4
     */
    public void l7dlog(Priority priority, String key, Object[] params, Throwable t) {
        if (repository.isDisabled(priority.level)) {
            return;
        }
        if (priority.isGreaterOrEqual(getEffectiveLevel())) {
            String pattern = getResourceBundleString(key);
            String msg;
            if (pattern == null) msg = key;
            else msg = java.text.MessageFormat.format(pattern, params);
            forcedLog(FQCN, priority, msg, t);
        }
    }

    /**
     * This generic form is intended to be used by wrappers.
     */
    public void log(Priority priority, Object message, Throwable t) {
        if (repository.isDisabled(priority.level)) {
            return;
        }
        if (priority.isGreaterOrEqual(getEffectiveLevel())) forcedLog(FQCN, priority, message, t);
    }

    /**
     * This generic form is intended to be used by wrappers.
     */
    public void log(Priority priority, Object message) {
        if (repository.isDisabled(priority.level)) {
            return;
        }
        if (priority.isGreaterOrEqual(getEffectiveLevel())) forcedLog(FQCN, priority, message, null);
    }

    /**
     * This is the most generic printing method. It is intended to be invoked by <b>wrapper</b> classes.
     *
     * @param callerFQCN The wrapper class' fully qualified class name.
     * @param level The level of the logging request.
     * @param message The message of the logging request.
     * @param t The throwable of the logging request, may be null.
     */
    public void log(String callerFQCN, Priority level, Object message, Throwable t) {
        if (repository.isDisabled(level.level)) {
            return;
        }
        if (level.isGreaterOrEqual(getEffectiveLevel())) {
            forcedLog(callerFQCN, level, message, t);
        }
    }

    /**
     * LoggerRepository forgot the fireRemoveAppenderEvent method, if using the stock Hierarchy implementation, then
     * call its fireRemove. Custom repositories can implement HierarchyEventListener if they want remove notifications.
     *
     * @param appender appender, may be null.
     */
    private void fireRemoveAppenderEvent(final Appender appender) {
        if (appender != null) {
            if (repository instanceof Hierarchy) {
                ((Hierarchy) repository).fireRemoveAppenderEvent(this, appender);
            } else if (repository instanceof HierarchyEventListener) {
                ((HierarchyEventListener) repository).removeAppenderEvent(this, appender);
            }
        }
    }

    /**
     * Remove all previously added appenders from this Category instance.
     * <p/>
     * <p>This is useful when re-reading configuration information.
     */
    synchronized
    public void removeAllAppenders() {
        if (aai != null) {
            Vector appenders = new Vector();
            for (Enumeration iter = aai.getAllAppenders(); iter != null && iter.hasMoreElements(); ) {
                appenders.add(iter.nextElement());
            }
            aai.removeAllAppenders();
            for (Enumeration iter = appenders.elements(); iter.hasMoreElements(); ) {
                fireRemoveAppenderEvent((Appender) iter.nextElement());
            }
            aai = null;
        }
    }

    /**
     * Remove the appender passed as parameter form the list of appenders.
     *
     * @since 0.8.2
     */
    synchronized
    public void removeAppender(Appender appender) {
        if (appender == null || aai == null) return;
        boolean wasAttached = aai.isAttached(appender);
        aai.removeAppender(appender);
        if (wasAttached) {
            fireRemoveAppenderEvent(appender);
        }
    }

    /**
     * Remove the appender with the name passed as parameter form the list of appenders.
     *
     * @since 0.8.2
     */
    synchronized
    public void removeAppender(String name) {
        if (name == null || aai == null) return;
        Appender appender = aai.getAppender(name);
        aai.removeAppender(name);
        if (appender != null) {
            fireRemoveAppenderEvent(appender);
        }
    }

    /**
     * Set the additivity flag for this Category instance.
     *
     * @since 0.8.1
     */
    public void setAdditivity(boolean additive) {
        this.additive = additive;
    }

    /**
     * Only the Hiearchy class can set the hiearchy of a category. Default package access is MANDATORY here.
     */
    final void setHierarchy(LoggerRepository repository) {
        this.repository = repository;
    }

    /**
     * Set the level of this Category. If you are passing any of <code>Level.DEBUG</code>, <code>Level.INFO</code>,
     * <code>Level.WARN</code>, <code>Level.ERROR</code>, <code>Level.FATAL</code> as a parameter, you need to case them
     * as Level.
     * <p/>
     * <p>As in <pre> &nbsp;&nbsp;&nbsp;logger.setLevel((Level) Level.DEBUG); </pre>
     * <p/>
     * <p/>
     * <p>Null values are admitted.
     */
    public void setLevel(Level level) {
        this.level = level;
    }

    /**
     * Set the level of this Category.
     * <p/>
     * <p>Null values are admitted.
     *
     * @deprecated Please use {@link #setLevel} instead.
     */
    public void setPriority(Priority priority) {
        level = (Level) priority;
    }

    /**
     * Set the resource bundle to be used with localized logging methods {@link #l7dlog(org.apache.log4j.Priority,
     * String, Throwable)} and {@link #l7dlog(org.apache.log4j.Priority, String, Object[], Throwable)}.
     *
     * @since 0.8.4
     */
    public void setResourceBundle(ResourceBundle bundle) {
        resourceBundle = bundle;
    }

    /**
     * Calling this method will <em>safely</em> close and remove all appenders in all the categories including root
     * contained in the default hierachy.
     * <p/>
     * <p>Some appenders such as {@link org.apache.log4j.net.SocketAppender} and {@link org.apache.log4j.AsyncAppender}
     * need to be closed before the application exists. Otherwise, pending logging events might be lost.
     * <p/>
     * <p>The <code>shutdown</code> method is careful to close nested appenders before closing regular appenders. This
     * is allows configurations where a regular appender is attached to a category and again to a nested appender.
     *
     * @since 1.0
     * @deprecated Please use {@link org.apache.log4j.LogManager#shutdown()} instead.
     */
    public
    static void shutdown() {
        LogManager.shutdown();
    }

    /**
     * Log a message object with the {@link org.apache.log4j.Level#WARN WARN} Level.
     * <p/>
     * <p>This method first checks if this category is <code>WARN</code> enabled by comparing the level of this category
     * with {@link org.apache.log4j.Level#WARN WARN} Level. If the category is <code>WARN</code> enabled, then it
     * converts the message object passed as parameter to a string by invoking the appropriate {@link
     * org.apache.log4j.or.ObjectRenderer}. It proceeds to call all the registered appenders in this category and also
     * higher in the hieararchy depending on the value of the additivity flag.
     * <p/>
     * <p><b>WARNING</b> Note that passing a {@link Throwable} to this method will print the name of the Throwable but
     * no stack trace. To print a stack trace use the {@link #warn(Object, Throwable)} form instead.  <p>
     *
     * @param message the message object to log.
     */
    public void warn(Object message) {
        if (repository.isDisabled(Level.WARN_INT)) return;

        if (Level.WARN.isGreaterOrEqual(getEffectiveLevel())) forcedLog(FQCN, Level.WARN, message, null);
    }

    /**
     * Log a message with the <code>WARN</code> level including the stack trace of the {@link Throwable} <code>t</code>
     * passed as parameter.
     * <p/>
     * <p>See {@link #warn(Object)} for more detailed information.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    public void warn(Object message, Throwable t) {
        if (repository.isDisabled(Level.WARN_INT)) return;
        if (Level.WARN.isGreaterOrEqual(getEffectiveLevel())) forcedLog(FQCN, Level.WARN, message, t);
    }
}
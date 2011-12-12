/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.grizzly.asyncqueue;

import java.io.IOException;
import java.util.concurrent.Future;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.Interceptor;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.Writer;
import java.net.SocketAddress;

/**
 * The {@link AsyncQueue}, which implements asynchronous write queue.
 *
 * @author Alexey Stashok
 * @author Ryan Lubke
 */
public interface AsyncQueueWriter<L> 
        extends Writer<L>, AsyncQueue {
    /**
     * Method writes the {@link Buffer} to the specific address.
     *
     *
     * @param connection the {@link org.glassfish.grizzly.Connection} to write to
     * @param dstAddress the destination address the {@link WriteQueueMessage} will be
     *        sent to
     * @param message the {@link WriteQueueMessage}, from which the data will be written
     * @param completionHandler {@link org.glassfish.grizzly.CompletionHandler},
     *        which will get notified, when write will be completed
     * @param interceptor {@link org.glassfish.grizzly.Interceptor}, which will 
     *        be able to intercept control each time new portion of a data was 
     *        written from a {@link WriteQueueMessage}.
     *        The {@link Interceptor} can decide, whether asynchronous write is
     *        completed or not, or provide other processing instructions.
     * @param cloner {@link MessageCloner}, which will be invoked by
     *        <tt>AsyncQueueWriter</tt>, if message could not be written to a
     *        channel directly and has to be put on a asynchronous queue    
     * @return {@link Future}, using which it's possible to check the
     *         result
     * @throws java.io.IOException
     */
    public GrizzlyFuture<WriteResult<WriteQueueMessage, SocketAddress>> write(
            Connection connection, SocketAddress dstAddress, WriteQueueMessage message,
            CompletionHandler<WriteResult<WriteQueueMessage, SocketAddress>> completionHandler,
            Interceptor<WriteResult<WriteQueueMessage, SocketAddress>> interceptor,
            MessageCloner<WriteQueueMessage> cloner)
            throws IOException;

    /**
     * @param connection the {@link Connection} to test whether or not the
     *  specified number of bytes can be written to.
     * @param size number of bytes to write.
     * @return <code>true</code> if the queue has not exceeded it's maximum
     *  size in bytes of pending writes, otherwise <code>false</code>
     */
    boolean canWrite(final Connection connection, int size);

    /**
     * Configures the maximum number of bytes pending to be written
     * for a particular {@link Connection}.
     *
     * @param maxQueuedWrites maximum number of bytes that may be pending to be
     *  written to a particular {@link Connection}.
     */
    void setMaxPendingBytesPerConnection(final int maxQueuedWrites);


    /**
     * @return the maximum number of bytes that may be pending to be written
     *  to a particular {@link Connection}.  By default, this will be four
     *  times the size of the {@link java.net.Socket} send buffer size.
     */
    int getMaxPendingBytesPerConnection();
    
    /**
     * Returns the maximum number of write() method reentrants a thread
     * is allowed to made.
     * This is related to possible write()->onComplete()->write()->...
     * chain, which may grow infinitely and cause StackOverflow.
     * Using maxWriteReentrants value it's possible to limit such a chain.
     *
     * @return the maximum number of write() method reentrants a thread
     * is allowed to make.
     */
    int getMaxWriteReentrants();

    /**
     * Sets the maximum number of write() method reentrants a thread
     * is allowed to made.
     * This is related to possible write()->onComplete()->write()->...
     * chain, which may grow infinitely and cause StackOverflow.
     * Using maxWriteReentrants value it's possible to limit such a chain.
     *
     * @param maxWriteReentrants  the maximum number of write() method calls
     *  a thread is allowed to make.
     */
    void setMaxWriteReentrants(int maxWriteReentrants);
    
    /**
     * Returns the current write reentrants counter.
     * Might be useful, if developer wants to use custom notification mechanism,
     * based on on {@link #canWrite(org.glassfish.grizzly.Connection, int)} and
     * various write methods.
     */
    Reentrant getWriteReentrant();

    /**
     * Returns <tt>true</tt>, if max number of write->completion-handler reentrants
     * has been reached for the passed {@link Reentrant} object, and next write
     * will happen in the separate thread.
     * 
     * @param reentrant {@link Reentrant} object.
     * @return <tt>true</tt>, if max number of write->completion-handler reentrants
     * has been reached for the passed {@link Reentrant} object, and next write
     * will happen in the separate thread.
     */
    boolean isMaxReentrantsReached(Reentrant reentrant);

    /**
     * Write reentrants counter
     */
    public static final class Reentrant {
        private int counter;
        
        public int get() {
            return counter;
        }
        
        public int incAndGet() {
            return ++counter;
        }

        public int decAndGet() {
            return --counter;
        }
    }    
}
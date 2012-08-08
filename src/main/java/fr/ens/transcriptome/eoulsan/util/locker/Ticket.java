/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.util.locker;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

/**
 * This class define a ticket for the TicketLocker.
 * @since 1.1
 * @author Laurent Jourdren
 */
public final class Ticket implements Comparable<Ticket>, Serializable {

  static final long serialVersionUID = 2737693612431974762L;

  private static final long initTime = System.currentTimeMillis();

  private final int pid;
  private final long threadId;
  private final long creationTime;
  private final long nanoCreationTime;
  private long lastActiveTime;
  private boolean working;

  //
  // Getter
  //

  public int getPid() {
    return this.pid;
  }

  public long getThreadId() {
    return this.threadId;
  }

  public long getCreationTime() {
    return this.creationTime;
  }

  public long getLastActiveTime() {
    return this.lastActiveTime;
  }

  public boolean isWorking() {
    return this.working;
  }

  //
  // Setter
  //

  public void setWorking(final boolean working) {
    this.working = working;
  }

  public void updateLastActiveTime() {
    this.lastActiveTime = System.currentTimeMillis();
  }

  //
  // Other methods
  //

  @Override
  public boolean equals(final Object o) {

    if (o == this)
      return true;

    if (o == null || !(o instanceof Ticket))
      return false;

    final Ticket t = (Ticket) o;

    return this.creationTime == t.creationTime
        && this.nanoCreationTime == t.nanoCreationTime && this.pid == t.pid
        && this.threadId == t.threadId;
  }

  @Override
  public int hashCode() {

    return hashCode(this.creationTime, this.nanoCreationTime, this.pid,
        this.threadId);
  }

  @Override
  public int compareTo(final Ticket ticket) {

    if (ticket == null)
      return 1;

    // Compare creation time
    final int comp1 =
        Long.valueOf(this.creationTime).compareTo(ticket.creationTime);
    if (comp1 != 0)
      return comp1;

    // Compare Nano creation time
    final int comp2 =
        Long.valueOf(this.nanoCreationTime).compareTo(ticket.nanoCreationTime);
    if (comp2 != 0)
      return comp2;

    // Compare PID
    final int comp3 = Integer.valueOf(this.pid).compareTo(this.pid);
    if (comp3 != 0)
      return comp3;

    return Long.valueOf(this.threadId).compareTo(this.threadId);
  }

  @Override
  public String toString() {

    return (this.creationTime - initTime)
        + " [" + this.pid + '.' + this.threadId + "]";
  }

  //
  // Static methods
  //

  public static int hashCode(Object... objects) {
    return Arrays.hashCode(objects);
  }

  private static final int getCurrentPid() {

    final String beanName = ManagementFactory.getRuntimeMXBean().getName();

    final int index = beanName.indexOf('@');

    return Integer.parseInt(beanName.substring(0, index));
  }

  //
  // Constructor
  //

  public Ticket() {
    this(getCurrentPid(), Thread.currentThread().getId(), System
        .currentTimeMillis(), System.nanoTime(), -1, false);
  }

  public Ticket(final Ticket ticket) {

    this(ticket.pid, ticket.threadId, ticket.creationTime,
        ticket.nanoCreationTime, ticket.lastActiveTime, ticket.working);
  }

  public Ticket(final int pid, final long threadId, final long creationTime,
      final long nanoCreationTime, final long lastActiveTime,
      final boolean working) {

    this.pid = pid;
    this.threadId = threadId;
    this.creationTime = creationTime;
    this.nanoCreationTime = nanoCreationTime;
    this.lastActiveTime =
        lastActiveTime == -1 ? this.creationTime : lastActiveTime;
    this.working = working;
  }

}

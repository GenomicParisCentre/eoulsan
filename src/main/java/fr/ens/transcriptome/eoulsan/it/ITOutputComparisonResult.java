package fr.ens.transcriptome.eoulsan.it;

/**
 * The class represents output result of file comparison.
 * @author Sandrine Perrin
 * @since 2.0
 */
final class ITOutputComparisonResult implements
    Comparable<ITOutputComparisonResult> {

  private String filename;
  // Init status comparison at to compare
  private StatusComparison statusComparison = StatusComparison.TO_COMPARE;
  private String message = "none";

  /**
   * Get report on comparison.
   * @return report on comparison
   */
  public String getReport() {
    return getStatutComparison() + " : " + filename + " " + getMessage();
  }

  /**
   * Set comparison result.
   * @param statusComparison status comparison object
   * @param message detail on comparison
   */
  public void setResult(final StatusComparison statusComparison,
      final String message) {
    setStatutComparison(statusComparison);
    setMessage(message);
  }

  @Override
  public int compareTo(final ITOutputComparisonResult that) {
    
    return this.filename.compareTo(that.filename);
  }

  
  //
  // Getters & setters
  //

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((filename == null) ? 0 : filename.hashCode());
    result = prime * result + ((message == null) ? 0 : message.hashCode());
    result =
        prime
            * result
            + ((statusComparison == null) ? 0 : statusComparison.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ITOutputComparisonResult other = (ITOutputComparisonResult) obj;
    if (filename == null) {
      if (other.filename != null) {
        return false;
      }
    } else if (!filename.equals(other.filename)) {
      return false;
    }
    if (message == null) {
      if (other.message != null) {
        return false;
      }
    } else if (!message.equals(other.message)) {
      return false;
    }
    if (statusComparison != other.statusComparison) {
      return false;
    }
    return true;
  }

  /**
   * Gets the filename.
   * @return the filename
   */
  public String getFilename() {
    return filename;
  }

  /**
   * Gets the statut comparison.
   * @return the statut comparison
   */
  public StatusComparison getStatutComparison() {
    return statusComparison;
  }

  /**
   * Gets the message.
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets the filename.
   * @param filename the new filename
   */
  public void setFilename(final String filename) {
    this.filename = filename;
  }

  /**
   * Sets the statut comparison.
   * @param statutComparison the new statut comparison
   */
  public void setStatutComparison(final StatusComparison statutComparison) {
    this.statusComparison = statutComparison;
  }

  /**
   * Sets the message.
   * @param message the new message
   */
  public void setMessage(String message) {
    this.message = message;
  }

  //
  // Constructors
  //
  /**
   * Public constructor.
   * @param filename filename to compare
   * @param statusComparison status comparison object
   * @param message detail of comparison
   */
  public ITOutputComparisonResult(final String filename,
      final StatusComparison statusComparison, final String message) {
    this.filename = filename;
    this.message = message;
    this.statusComparison = statusComparison;
  }

  /**
   * Public constructor.
   * @param filename filename to compare
   */
  public ITOutputComparisonResult(final String filename) {
    this.filename = filename;
  }

  //
  // Internal class
  //

  /**
   * The class define status comparison available to compare files.
   * @author Sandrine Perrin
   */
  enum StatusComparison {

    NOT_EQUALS("not equals", false,
        "Comparison(s) failed for output result file(s): "), EQUALS("equals",
        true, ""), UNEXPECTED("unexpected", false,
        "Found unexpected file(s) in result test directory: "),
    MISSING("missing", false,
        "Missing expected file(s) in result test directory: "), TO_COMPARE(
        "to compare", false, "Not comparison to start.");

    private final String name;
    private final String exceptionMessage;
    private final boolean isSuccess;

    public String getName() {
      return name;
    }

    public boolean isSuccess() {
      return isSuccess;
    }

    public String getExceptionMessage() {
      return this.exceptionMessage;
    }

    StatusComparison(final String name, final boolean isSuccess,
        final String exceptionMessage) {
      this.name = name;
      this.isSuccess = isSuccess;
      this.exceptionMessage = exceptionMessage;
    }
  }
}
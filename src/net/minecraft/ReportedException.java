package net.minecraft;

public class ReportedException extends RuntimeException {
   private final net.minecraft.CrashReport report;

   public ReportedException(net.minecraft.CrashReport p_134760_) {
      this.report = p_134760_;
   }

   public net.minecraft.CrashReport getReport() {
      return this.report;
   }

   @Override
   public Throwable getCause() {
      return this.report.getException();
   }

   @Override
   public String getMessage() {
      return this.report.getTitle();
   }
}

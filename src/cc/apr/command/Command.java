package cc.apr.command;

public interface Command {
   String getName();

   String getDescription();

   String getUsage();

   void execute(String[] var1);
}

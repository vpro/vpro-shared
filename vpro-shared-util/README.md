# Shared utilities

Simple utilities with little or no dependencies themselves

E.g. 
- Collection related, like CountedIterator, ClosableIterator, SkippingIterator, TransformingList
- Strings related, like TextUtil, HTMLStripper, Strings
- Very basic interfaces: TriFunction, Pair
- JMX related: `nl.vpro.jmx`
- CommandExecutor to convientiently call external commands
- ...

## CommandExecutor
```java
 // initialize
 CommandExecutor convert = CommandExecutorImpl
                 .builder()
                 .executablesPaths("/opt/local/bin/convert", "/bin/convert", "/usr/local/bin/convert")
                 .commonArg("-")
                 .commonArg("png:-")
                 .logger(log)
                 .wrapLogInfo(CharSequence::toString)
                 .build();
  
  
  //use 
  File out = File.createTempFile(FilenameUtils.removeExtension(pdfFileName), ".png");
  try (OutputStream outputStream = new FileOutputStream(out)) {
      int exitCode = convert.execute(zip, outputStream, LoggerOutputStream.error(log));
      if (exitCode != 0) {
           log.warn("Exit code: {}", exitCode);
      }
  }
```

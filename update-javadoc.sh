# Run this once in a while to update javadoc on github pages
mvn clean javadoc:aggregate
rm -rf docs/*
mv target/site/apidocs/* docs

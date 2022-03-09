del bootstrap.jar
jar cvf0 bootstrap.jar -C out/production/jerry cn/wb/jerry/BootStrap.class -C out/production/jerry cn/wb/jerry/classloader/CommonClassLoader.class
del lib/jerry.jar
cd out
cd production
cd jerry
jar cvf0 ../../../lib/jerry.jar *
cd ..
cd ..
cd ..
java -cp bootstrap.jar cn.wb.jerry.BootStrap
pause
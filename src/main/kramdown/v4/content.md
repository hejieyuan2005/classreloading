Java Class reloading tutorial
=====

Java Class reloading has always been a very difficult technique, not only because very little document explaining this process, but also it require a very clear and careful system design. In this tutorial, I would like to provide a step by step explanation of this process and help you master this infamous technique.

Work-space setup
------------

This tutorial require Maven, Git and either Eclipse or IntelliJ IDEA.

All source code for this tutorial is uploaded at this GitHub:
[https://github.com/quanla/classreloading](https://github.com/quanla/classreloading). Please check it out to your local hard drive, we will need to set up a proper workspace.

### If you are using Eclipse: 
- Run command: `mvn eclipse:eclipse` to generate Eclipse's project files
- Load the generated project.
- Set output path to `target/classes`

### If you are using IntelliJ: 

- Import the project's pom file.
- IntelliJ will not auto-compile when you are running any example inside it, so you have to choose either:
 - Run the examples inside IntelliJ, then every time you want to compile, you'll have to press `Alt+B E`
 - Run the examples outside IntelliJ with the run_example*.bat; Set IntelliJ compiler's auto-compile to true then every time you change any java file, IntelliJ will auto-compile it.


Example 1: StaticInt
-------------

Source code: `src/main/java/qj/blog/classreloading/example1/StaticInt.java`

The first example will give you a general understanding of Class Loader:

~~~ java

Class<?> userClass1 = User.class;
Class<?> userClass2 = new DynamicClassLoader("target/classes")
		.load("qj.blog.classreloading.example1.StaticInt$User");

~~~

In this example, there will be 2 User classes loaded into the memory. The `userClass1` will be loaded by the JVM's default class loader, the second one using the `DynamicClassLoader`, a custom class loader whose source code also provided in the **GitHub** project.

   
Here is the rest of the source code:

~~~

out.println("Seems to be the same class:");
out.println(userClass1.getName());
out.println(userClass2.getName());
out.println();

out.println("But why there are 2 different class loaders:");
out.println(userClass1.getClassLoader());
out.println(userClass2.getClassLoader());
out.println();

User.age = 11;
out.println("And different age values:");
out.println((int) ReflectUtil.getStaticFieldValue("age", userClass1));
out.println((int) ReflectUtil.getStaticFieldValue("age", userClass2));

~~~

And the output:

~~~

Seems to be the same class:
qj.blog.classreloading.example1.StaticInt$User
qj.blog.classreloading.example1.StaticInt$User

But why there are 2 different class loaders:
qj.util.lang.DynamicClassLoader@3941a79c
sun.misc.Launcher$AppClassLoader@1f32e575

And different age values:
11
10

~~~

So as you can see here, although the `User` classes have same name, they are actually 2 different classes, and they can be managed, manipulated independently. The age value, although declared as static, exists in 2 versions, attaching to each classes and can be changed independently too.

<< image `1_2_loaders.jpg` >>

A word about the `ClassLoader`: they are the portal bringing classes into the JVM. When one class require another class to be loaded, it's the ClassLoader's task to do the loading.

In this example, the custom `ClassLoader` named `DynamicClassLoader` is used to load the second version of User class. If instead of `DynamicClassLoader`, we use the default class loader ( with command: `StaticInt.class.getClassLoader()` ) then the same User class will be used, as all loaded classes are cached.

#### The DynamicClassLoader

The `DynamicClassLoader` is in a sense the most important part of this article, so we must understand how it works before making anything possible.

First of all, there can be multiple class loaders in a normal Java program. The one that loads your main class is the default one, and from your code, you can create and use as many more class loaders as you like to.

About loading behavior: A default/normal class loader would give its parent `ClassLoader` the priority and only load classes that its parent can not load. That is suitable for normal circumstances, but not in our case. Unlike the default behavior of a ClassLoaders, our `DynamicClassLoader` has a more aggressive strategy. The `DynamicClassLoader` on the other hand will try to look through all its class paths and resolve the target class before it give up the right to its parent.

This is the code snippet in the `AggressiveClassLoader`, the parent class of `DynamicClassLoader`, which tells why it's **aggressive**:

~~~ java
byte[] newClassData = loadNewClass(name);
if (newClassData != null) {
	loadedClasses.add(name);
	return loadClass(newClassData, name);
} else {
	unavaiClasses.add(name);
	return parent.loadClass(name);
}
~~~

Few more things about this Dynamic Class Loader:
- The loaded classes have the same performance and other attributes as other classes loaded by the default class loader.
- The Dynamic Class Loader can be Garbage Collected together with all of it's loaded classes and objects.

In this example 1, the Dynamic ClassLoader is created with only 1 class path: "target/classes" ( in our current directory ), so it's capable of loading all the classes reside in that classpath. For all the classes not in there, it will have to refer to the parent class loader. For example we need to load String class in our StaticInt class, and our class loader does not have access to the rt.jar in our jre folder, so the String class of the parent class loader will be used.

With the ability to load and use 2 versions of the same class, we are now thinking of dumping the old version and load the new one to replace it. In the next example, we will do that... continuously.


Example 2: ReloadingContinuously
================

Source code: `src/main/java/qj/blog/classreloading/example2/ReloadingContinuously.java`

This example will show you that the JRE can load and reload classes forever, with old classes dumped and garbage collected, and brand new class loaded from hard drive and put to use.

~~~ java

Class<?> userClass = new DynamicClassLoader("target/classes")
  .load("qj.blog.classreloading.example2.ReloadingContinuously$User");
ReflectUtil.invokeStatic("hobby", userClass);
ThreadUtil.sleep(2000);

~~~

With every 2 seconds, the old `User` class will be dumped, a new one will be loaded and method `hobby` is invoked.

Here is the `User` class definition:

~~~ java
@SuppressWarnings("UnusedDeclaration")
public static class User {
	public static void hobby() {
		playFootball(); // Will comment during runtime
//		playBasketball(); // Will uncomment during runtime
	}
	
	// Will comment during runtime
	public static void playFootball() {
		System.out.println("Play Football");
	}
	
	// Will uncomment during runtime
//	public static void playBasketball() {
//		System.out.println("Play Basketball");
//	}
}
~~~

When running this application, you should try to comment and uncomment the code in User class, you will see that the newest definition will always be used.

Here is the example output:

~~~
...
Play Football
Play Football
Play Football
Play Basketball
Play Basketball
Play Basketball

~~~

Every time a new instance of `DynamicClassLoader` is created, and it will load the `User` class in the "`target/classes`" folder which we had set Eclipse or IntelliJ to output the latest class file. All old  `DynamicClassLoader`s and old `User` classes will be unlinked and subjected to be garbage-collected.

<< Image `2_many_loaders.jpg` >>

If you are familiar with JVM HotSpot, then it's noteworthy here that the class structure can also be changed and reloaded: playFootball method is to be removed and playBasketball method is to be added. This is different to HotSpot, which allow only method content be changed, or the class can not be reloaded.

Now that we are capable of reloading a class, it is time to try reloading many classes at once. Let's try it out in the next example.


Example 3: ContextReloading
================

Source code: `src/main/java/qj/blog/classreloading/example3/ContextReloading.java`

The output of this example will be the same with example 2, the only difference is its more application-like structure with context, service and model objects...  

This example's source code is rather large, so I only put here parts of it, for the actual source code file, please refer to the GitHub project.

The `main` method, which create the reloading cycles:

~~~ java
for (;;) {
	Object context = createContext();
	invokeHobbyService(context);
	ThreadUtil.sleep(2000);
}
~~~

Method `createContext`, which create new `DynamicClassLoader` and create the context object from it:

~~~ java
Class<?> contextClass = new DynamicClassLoader("target/classes")
	.load("qj.blog.classreloading.example3.ContextReloading$Context");
Object context = newInstance(contextClass);
invoke("init", context);
return context;

~~~

Method `invokeHobbyService`, which put the context object to use:

~~~ java
Object hobbyService = getFieldValue("hobbyService", context);
invoke("hobby", hobbyService);
~~~

And here is the `Context` class, which describe how it links to the service:

~~~ java
public static class Context {
	public HobbyService hobbyService = new HobbyService();
	
	public void init() {
		// Init your services here
		hobbyService.user = new User();
	}
}
~~~

Please note that in this `Context` class, the code no longer requires to use Reflection, as everything is now loaded in the same `DynamicClassLoader`, they linked together, live and die together, so they can freely use direct link.

The `Context` class in this example is much more complicated than the `User` class in the previous examples: it has link to other classes, it has the `init` method to be called every it is instantiated. Basically, it's very similar to real world application's context classes, (which keep hold of the application's modules and do dependency injection). So being able to reload this `Context` class together with all it's linked classes is a great step toward applying this technique to real life.

<< image `3_context_reloading.jpg`  >>

As the number of classes and objects grow, our step of "drop old versions" will also be more complicated, this is also the biggest reason why class reloading is so difficult. To possibly drop old versions we will have to make sure that once the new context created all the references to the old classes and objects are dropped. Soon I will show you the way to deal with it elegantly:

The `main` method here will have a hold of the context object, and **that is the only link** to all the things that need to be dropped. If we break that link, the context object and the context class, and the service object ... will all be subjected to the `Garbage Collector`.

A little explanation about why normally classes are so persistent and not get Garbage Collected:

- Normally we load all our classes into the default classloader
- The class - class loader is a 2-ways relationship with the classloader also cache all the classes it has loaded
- So as long as the default classloader still connected to any alive thread, everything (all loaded classes) will be immune to the Garbage Collector.
- That said, unless we can separate the code we want to reload with the one already loaded by the default class loader, our new code changes will never be applied during runtime.

With this example, we see that reloading all application's classes is pretty easy, which is merely about keeping a thin, dropable connection from the live thread to the dynamic class loader in use. But what if we want some objects (and their classes) not to be reloaded and be reused between reloading cycles? Let's look at the next example.


Example 4: KeepConnectionPool
================

Source code: `src/main/java/qj/blog/classreloading/example4/KeepConnectionPool.java`

The `main` method:

~~~ java

ConnectionPool pool = new ConnectionPool();

for (;;) {
	Object context = createContext(pool);

	invokeService(context);

	ThreadUtil.sleep(2000);
}

~~~

So you can see that the trick here is loading the `ConnectionPool` class and instantiate it outside the reloading cycle, keeping it in the persisted space, and pass the reference to the `Context` objects

The `createContext` method is also a little bit different:

~~~ java

ExceptingClassLoader classLoader = new ExceptingClassLoader(
		(className) -> className.contains("$Connection"),
		"target/classes");
Class<?> contextClass = classLoader.load(KeepConnectionPool.class.getName() + "$Context");
Object context = newInstance(contextClass);

setFieldValue(pool, "pool", context);
invoke("init", context);

return context;

~~~

From now on, we will call the objects and classes that are reloaded with every cycle the "Reloadable space" and others the "Persisted space", which contain the objects and classes not recycled and not renewed during the reloading cycles. We will have to be very clear about which object or class stay in which space, thus drawing a separation line between these 2 spaces.

<< Image `4_persisting_connection.jpg` >>

As seen from the picture, not only the Context Object and the UserService Object is referring to the ConnectionPool Object, but the Context and UserService classes are also referring to the ConnectionPool class. This is a very dangerous situation which often lead to confusion and failure. The ConnectionPool class must not be loaded by our `DynamicClassLoader`, there must be only one `ConnectionPool` class in the memory, which is the one loaded by the default `ClassLoader`.

What if our `DynamicClassLoader` accidentally load the `ConnectionPool` class? Then the `ConnectionPool` object from the persisted space can not be passed to the Context object, because the Context object is expecting an object of a different class, which named `ConnectionPool` too, but is actually a different class.

So how do we prevent our `DynamicClassLoader` from loading the `ConnectionPool` class? Instead of using `DynamicClassLoader`, this example use a subclass of it named: `ExceptingClassLoader`, which will pass the loading to super classloader base on a condition function:

~~~ java
(className) -> className.contains("$Connection")
~~~

If we don't use `ExceptingClassLoader` here, then the `DynamicClassLoader` would load the `ConnectionPool` class because that class reside in the "`target/classes`" folder. Another way to prevent the `ConnectionPool` class to be picked up by our `DynamicClassLoader` is to compile the `ConnectionPool` class to a different folder, maybe put to a different module and it will be compiled separately.

#### Rules for choosing space

Now, the job gets really confusing, how to determine which classes would be in Persisted space, and which classes in Reloadable space. Here are the rules:

- 1 class can serve in either or both Persisted space and Reloadable space if objects of its type are not sent across the 2 spaces. For example: utility classes with all static methods like `StringUtils`
- 1 class has objects sent across the 2 spaces must be in Persisted space - like the `ConnectionPool` class. All linked classes must also be in Persisted space - like the `Connection` class which is linked from `ConnectionPool`.

So you can see that the rules are not very restricted, except for the crossing classes that has objects transfer across the 2 spaces, all other classes can be freely used in either Persisted space or Reloadable space or both. Of course only classes in Reloadable space will enjoy being reloaded with reloading cycles.

So the most challenging problem with Class Reloading is dealt with. In the next example, we will try to apply this technique to a simple Web Application, and enjoy the reloading Java classes just like any scripting language

Example 5: Little Phone Book
================

Source code: `src/main/java/qj/blog/classreloading/example5/LittlePhoneBookMain.java`

This example will be very similar to what a normal web application should look like. It is a Single Page Application with AngularJS, db is SQLite, Maven, and Jetty Embedded Web Server.

Here is the reloadable space in the web server's structure:

<< Image `5_web_app.jpg` >>

The web server will not hold reference to the real servlets (which must stay in the reloadable space, so to be reloaded ), what it holds is stub servlets, which with every call to its service method, will resolve the actual servlet in the actual context to run.

This example also introduce a new object `ReloadingWebContext`, which provide to the Web Server all values like a normal Context, but internally hold reference to a actual context object that can be reloaded by a `DynamicClassLoader`. It is this `ReloadingWebContext` which provide stub servlets to the web server.

<< Image `5_reloading_web_context.jpg` >>

The `ReloadingWebContext` will be the wrapper of the actual context, which will:

- Reload the actual context when a HTTP GET to "/" in called.
- Provide Stub servlet to web server.
- Set values, invoke methods every time the actual context is initialized or destroyed.
- Can be configured to reload context or not, and which ClassLoader is used for reloading. This will help when running the application in Production.

Because it's very important to understanding how we isolate the Persisted space and Reloadable space, here are the 2 classes that are crossing between the 2 spaces:

- Class `qj.util.funct.F0` for object `public F0<Connection> connF` in `Context`: Function object, will return a Connection each time the function is invoked. This class resides in the qj.util package, which is excluded from the `DynamicClassLoader`.
- Class `java.sql.Connection` for object `public F0<Connection> connF` in `Context`: Normal sql connection object. This class does not reside in our `DynamicClassLoader`'s class path so it won't be picked up.










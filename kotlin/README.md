Kotlin ResultSet mapper for jDBI
================================

This plugin adds ResultSet mapping to Kotlin classes anywhere a ResultSet is used.

ResultSet mapping supports data classes where all fields are present in the constructor as well as classes with 
writable properties. Any fields not present in the constructor will be set after the constructor call.
The mapper supports nullable types.  It also uses default parameter values in 
the constructor if the parameter type is not nullable and the value absent in the result set.

Result set mapper also supports `@ColumnName` annotation that allows to specify name for a property or parameter explicitly.


### Usage:

If you load all DBI plugins via
`Jdbi.installPlugins()` this plugin will be discovered and registered automatically.
Otherwise, you can attach the plugin using `Jdbi.installPlugin(KotlinPlugin())`.

An example from the test class:

```
    data class Thing(val id: Int, val name: String,
                     val nullable: String?,
                     val nullableDefaultedNull: String? = null,
                     val nullableDefaultedNotNull: String? = "not null",
                     val defaulted: String = "default value")

     @Test fun testFindById() {
    
            val qry = db.sharedHandle.createQuery("select id, name from something where id = :id")
            val things: List<Thing> = qry.bind("id", brian.id).mapTo<Thing>().list()
            assertEquals(1, things.size)
            assertEquals(brian, things[0])
    
     } 
```

There are two extensions to help:

* `<reified T : Any>ResultBearing.mapTo()`
* `<T : Any>ResultIterable<T>.useSequence(block: (Sequence<T>)->Unit)` 

Allowing code like:

```
val qry = handle.createQuery("select id, name from something where id = :id")
val things = qry.bind("id", brian.id).mapTo<Thing>.list()
```

and for using a Sequence that is auto closed:

```
qryAll.mapTo<Thing>.useSequence {
    it.forEach(::println)
}
```


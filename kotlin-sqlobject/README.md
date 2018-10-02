Kotlin support for Jdbi SqlObject
=================================

This plugin adds both automatic parameter binding by name for Kotlin methods and
support for Kotlin default methods in SqlObjects.

Parameter binding supports individual primitive types, and also Kotlin or JavaBean style objects as a parameter 
(referenced in binding as `:paramName.propertyName`).  No annotations are needed.

### Usage:

If you load all Jdbi plugins via 
`Jdbi.installPlugins()` this plugin will be discovered and registered automatically.
Otherwise, you can attach the plugin via:  `Jdbi.installPlugin(KotlinSqlObjectPlugin())`. 
This plugin works together with core jDbi Kotlin plugin, `KotlinPlugin`, that adds ResultSet mapping to Kotlin classes.

An example from the test class:

```
    data class Thing(val id: Int, val name: String,
                     val nullable: String?,
                     val nullableDefaultedNull: String? = null,
                     val nullableDefaultedNotNull: String? = "not null",
                     val defaulted: String = "default value")

    interface ThingDao : SqlObject {
        @SqlUpdate("insert into something (id, name) values (:something.id, :something.name)")
        fun insert(something: Thing)

        @SqlQuery("select id, name from something")
        fun list(): List<Thing>

        @SqlQuery("select id, name, null as nullable, null as nullableDefaultedNull, null as nullableDefaultedNotNull, 'test' as defaulted from something")
        fun listWithNulls(): List<Thing>

        @SqlQuery("select id, name from something where id=:id")
        fun findById(id: Int): Thing
        
        fun insertAndFind(something: Thing): Thing {
            insert(something)
            return findById(something.id)
        }
    }

    @Test fun testDao() {
      val dao = db.jdbi.onDemand<ThingDao>()

      val brian = Thing(1, "Brian", null)
      val keith = Thing(2, "Keith", null)

      dao.insert(brian)
      dao.insert(keith)

      val rs = dao.list()

      assertEquals(2, rs.size.toLong())
      assertEquals(brian, rs[0])
      assertEquals(keith, rs[1])

      val foundThing = dao.findById(2)
      assertEquals(keith, foundThing)

      val rs2 = dao.listWithNulls()
      assertEquals(2, rs2.size.toLong())
      assertEquals(brian.copy(nullable = null, nullableDefaultedNull = null, nullableDefaultedNotNull = null, defaulted = "test"), rs2[0])
      assertEquals(keith.copy(nullable = null, nullableDefaultedNull = null, nullableDefaultedNotNull = null, defaulted = "test"), rs2[1])
    }
    
    @Test
    fun testDefaultMethod() {
        val dao = db.jdbi.onDemand<ThingDao>()
        val brian = Thing(1, "Brian", null)

        val found = dao.insertAndFind(brian)
        assertEquals(brian, found)

    }
```




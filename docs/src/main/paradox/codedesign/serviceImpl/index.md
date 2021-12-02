# Service design without interface

## Cache implementation

The EhCacheService implementation as below:

@@snip[EhCacheService](code/EhCaheService.scala)

The service will not need the binding function in module like:

```scala
  override def configure(): Unit = {
    bind(classOf[Database]).toProvider(classOf[DatabaseProvider])
    bind(classOf[CityDAO]).to(classOf[SlickCityDAO])
      ....
```

But when using the service:

@@snip[ReadImpl](code/FundEngagementReportTypeSelectionReadImpl.scala)


Then the service is using the instance of the service not the interface.
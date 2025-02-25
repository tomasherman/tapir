# Creating your own Tapir

Tapir uses a number of packages which contain either the data classes for describing endpoints or interpreters
of this data (turning endpoints into a server or a client). Importing these packages every time you want to use Tapir
may be tedious, that's why each package object inherits all of its functionality from a trait.

Hence, it is possible to create your own object which combines all of the required functionalities and provides
a single-import whenever you want to use tapir. For example:

```scala
object MyTapir extends Tapir
  with AkkaHttpServerInterpreter
  with SttpClientInterpreter
  with OpenAPIDocsInterpreter
  with SchemaDerivation
  with TapirJsonCirce
  with TapirOpenAPICirceYaml
  with TapirAliases
```

Then, a single `import MyTapir._` and all Tapir data types and interpreter methods will be in scope!

You might also define an alias for `Endpoint`, with the capabilities that your endpoints use, e.g.:

```scala
import sttp.capabilities.akka.AkkaStreams
import sttp.capabilities.WebSockets
import sttp.tapir.Endpoint

type MyEndpoint[I, E, O] = Endpoint[I, E, O, AkkaStreams with WebSockets]
```
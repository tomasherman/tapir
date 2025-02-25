package sttp.tapir.client.tests

import cats.effect.unsafe.implicits.global
import sttp.model.{QueryParams, StatusCode}
import sttp.tapir._
import sttp.tapir.model.UsernamePassword
import sttp.tapir.tests.TestUtil.writeToFile
import sttp.tapir.tests._

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer

trait ClientBasicTests { this: ClientTests[Any] =>
  private val testFile = writeToFile("pen pineapple apple pen")

  def basicTests(): Unit = {
    testClient(endpoint, (), Right(()))
    testClient(in_query_out_string, "apple", Right("fruit: apple"))
    testClient(in_query_query_out_string, ("apple", Some(10)), Right("fruit: apple 10"))
    testClient(in_header_out_string, "Admin", Right("Role: Admin"))
    testClient(in_path_path_out_string, ("apple", 10), Right("apple 10 None"))
    testClient(in_string_out_string, "delicious", Right("delicious"))
    testClient(in_mapped_query_out_string, "apple".toList, Right("fruit: apple"))
    testClient(in_mapped_path_out_string, Fruit("kiwi"), Right("kiwi"))
    testClient(in_mapped_path_path_out_string, FruitAmount("apple", 10), Right("apple 10 None"))
    testClient(in_query_mapped_path_path_out_string, (FruitAmount("apple", 10), "red"), Right("apple 10 Some(red)"))
    testClient(in_query_out_mapped_string, "apple", Right("fruit: apple".toList))
    testClient(in_query_out_mapped_string_header, "apple", Right(FruitAmount("fruit: apple", 5)))
    testClient(in_json_out_json, FruitAmount("orange", 11), Right(FruitAmount("orange", 11)))
    testClient(in_byte_array_out_byte_array, "banana kiwi".getBytes(), Right("banana kiwi".getBytes()))
    testClient(in_byte_buffer_out_byte_buffer, ByteBuffer.wrap("mango".getBytes), Right(ByteBuffer.wrap("mango".getBytes)))
    testClient(
      in_input_stream_out_input_stream,
      new ByteArrayInputStream("mango".getBytes),
      Right(new ByteArrayInputStream("mango".getBytes))
    )
    testClient(in_file_out_file, testFile, Right(testFile))
    testClient(in_form_out_form, FruitAmount("plum", 10), Right(FruitAmount("plum", 10)))
    testClient(
      in_query_params_out_string,
      QueryParams.fromMap(Map("name" -> "apple", "weight" -> "42", "kind" -> "very good")),
      Right("kind=very good&name=apple&weight=42")
    )
    testClient(in_paths_out_string, List("fruit", "apple", "amount", "50"), Right("apple 50 None"))
    test(in_query_list_out_header_list.showDetail) {
      // Note: some clients do not preserve the order in header values
      send(
        in_query_list_out_header_list,
        port,
        List("plum", "watermelon", "apple")
      ).unsafeToFuture()
        .map(
          _.toOption.get should contain theSameElementsAs (
            // The fetch API merges multiple header values having the same name into a single comma separated value
            if (platformIsScalaJS)
              List("apple, watermelon, plum")
            else
              List("apple", "watermelon", "plum")
          )
        )
    }
    // cookie support in sttp is currently only available on the JVM
    if (!platformIsScalaJS) {
      test(in_cookie_cookie_out_header.showDetail) {
        send(
          in_cookie_cookie_out_header,
          port,
          (23, "pomegranate")
        ).unsafeToFuture().map(_.toOption.get.head.split(" ;") should contain theSameElementsAs "etanargemop=2c ;32=1c".split(" ;"))
      }
    }
    // TODO: test root path
    testClient(in_auth_apikey_header_out_string, "1234", Right("Authorization=None; X-Api-Key=Some(1234); Query=None"))
    testClient(in_auth_apikey_query_out_string, "1234", Right("Authorization=None; X-Api-Key=None; Query=Some(1234)"))
    testClient(
      in_auth_basic_out_string,
      UsernamePassword("teddy", Some("bear")),
      Right("Authorization=Some(Basic dGVkZHk6YmVhcg==); X-Api-Key=None; Query=None")
    )
    testClient(in_auth_bearer_out_string, "1234", Right("Authorization=Some(Bearer 1234); X-Api-Key=None; Query=None"))
    testClient(in_string_out_status_from_string.name("status one of 1"), "apple", Right(Right("fruit: apple")))
    testClient(in_string_out_status_from_string.name("status one of 2"), "papaya", Right(Left(29)))
    testClient(in_int_out_value_form_exact_match.name("first exact status of 2"), 1, Right("B"))
    testClient(in_int_out_value_form_exact_match.name("second exact status of 2"), 2, Right("A"))
    testClient(in_string_out_status, "apple", Right(StatusCode.Ok))

    testClient(delete_endpoint, (), Right(()))

    testClient(in_optional_json_out_optional_json.name("defined"), Some(FruitAmount("orange", 11)), Right(Some(FruitAmount("orange", 11))))
    testClient(in_optional_json_out_optional_json.name("empty"), None, Right(None))

    testClient(
      in_4query_out_4header_extended.in("api" / "echo" / "param-to-upper-header"),
      (("1", "2"), "3", "4"),
      Right((("1", "2"), "3", "4"))
    )

    testClient(out_json_or_default_json.name("person"), "person", Right(Person("mary", 20)))
    testClient(out_json_or_default_json.name("org"), "org", Right(Organization("work")))

    testClient(out_no_content_or_ok_empty_output.name("204"), 204, Right(()))
    testClient(out_no_content_or_ok_empty_output.name("200"), 200, Right(()))

    // when no content response should pick first non body mapping
    testClient(out_json_or_empty_output_no_content, 204, Right(Left(())))

    testClient(
      MultipleMediaTypes.out_json_xml_text_common_schema.name("json content organization"),
      "application/json",
      Right(Organization("sml"))
    )
    testClient(
      MultipleMediaTypes.out_json_xml_text_common_schema.name("xml content organization"),
      "application/xml",
      Right(Organization("sml-xml"))
    )

    testClient(
      MultipleMediaTypes.out_json_xml_different_schema.name("json content person"),
      "application/json",
      Right(Person("John", 21))
    )
    testClient(
      MultipleMediaTypes.out_json_xml_different_schema.name("xml content organization"),
      "application/xml",
      Right(Organization("sml-xml"))
    )

    test(in_headers_out_headers.showDetail) {
      send(
        in_headers_out_headers,
        port,
        List(sttp.model.Header("X-Fruit", "apple"), sttp.model.Header("Y-Fruit", "Orange"))
      ).unsafeToFuture()
        .map(_.toOption.get should contain allOf (sttp.model.Header("X-Fruit", "elppa"), sttp.model.Header("Y-Fruit", "egnarO")))
    }

    // the fetch API doesn't allow bodies in get requests
    if (!platformIsScalaJS) {
      test(in_json_out_headers.showDetail) {
        send(in_json_out_headers, port, FruitAmount("apple", 10))
          .unsafeToFuture()
          .map(_.toOption.get should contain(sttp.model.Header("Content-Type", "application/json".reverse)))
      }
    }

    testClient[Unit, Unit, Unit](in_unit_out_json_unit, (), Right(()))

    test(in_fixed_header_out_string.showDetail) {
      send(in_fixed_header_out_string, port, ())
        .unsafeToFuture()
        .map(_ shouldBe Right("Location: secret"))
    }

    // when there's a 404, fetch API seems to throw an exception, not giving us the opportunity to parse the result
    if (!platformIsScalaJS) {
      test("not existing endpoint, with error output not matching 404") {
        safeSend(not_existing_endpoint, port, ())
          .unsafeToFuture()
          .map(_ should matchPattern { case DecodeResult.Error(_, _: IllegalArgumentException) =>
          })
      }
    }
  }
}

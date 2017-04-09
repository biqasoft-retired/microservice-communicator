package com.biqasoft.microservice.communicator.interfaceimpl;

import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroMapping;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroPathVar;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.Microservice;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Created by ya on 3/25/2017.
 */
@Microservice("http://query.yahooapis.com/v1/public/")
public interface YahooCurrencyExchange {

    @MicroMapping("yql?q=select%20*%20from%20csv%20where%20url%3D%22http%3A%2F%2Fdownload.finance.yahoo.com%2Fd%2Fquotes.csv%3Fe%3D.csv%26f%3Dc4l1%26s%3D{from}{to}%253DX%252C%22%3B&format=json&diagnostics=true&callback=")
    JsonNode getExchangeRate(@MicroPathVar("from") String from, @MicroPathVar("to") String to);

    @MicroMapping(value = "yql?q=select%20*%20from%20csv%20where%20url%3D%22http%3A%2F%2Fdownload.finance.yahoo.com%2Fd%2Fquotes.csv%3Fe%3D.csv%26f%3Dc4l1%26s%3D{from}{to}%253DX%252C%22%3B&format=json&diagnostics=true&callback=", returnExpression = "query.results.row.col1")
    Double getExchangeRateAsDouble(@MicroPathVar("from") String from, @MicroPathVar("to") String to);

    @MicroMapping("yql?q=select%20*%20from%20xml%20where%20url%3D%27http%3A%2F%2Ffinance.yahoo.com%2Fwebservice%2Fv1%2Fsymbols%2Fallcurrencies%2Fquote%27&format=json")
    JsonNode getAllCurrencies();

}

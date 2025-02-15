/*
 * Ski Data API for NEU Seattle distributed systems course
 * An API for an emulation of skier managment system for RFID tagged lift tickets. Basis for CS6650 Assignments for 2019
 *
 * OpenAPI spec version: 2.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package io.swagger.client.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * ResortIDSeasonsBody
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2025-02-14T20:29:04.655340567Z[GMT]")

public class ResortIDSeasonsBody {
  @SerializedName("year")
  private String year = null;

  public ResortIDSeasonsBody year(String year) {
    this.year = year;
    return this;
  }

   /**
   * 4 character string specifying new season start year
   * @return year
  **/
  @Schema(example = "2019", description = "4 character string specifying new season start year")
  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    this.year = year;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResortIDSeasonsBody resortIDSeasonsBody = (ResortIDSeasonsBody) o;
    return Objects.equals(this.year, resortIDSeasonsBody.year);
  }

  @Override
  public int hashCode() {
    return Objects.hash(year);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ResortIDSeasonsBody {\n");
    
    sb.append("    year: ").append(toIndentedString(year)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

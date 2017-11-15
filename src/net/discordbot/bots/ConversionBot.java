package net.discordbot.bots;

import net.discordbot.common.DiscordBot;
import net.discordbot.common.TextListener;
import net.discordbot.util.Config;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.measure.Measure;
import javax.measure.converter.UnitConverter;

import static javax.measure.unit.NonSI.*;
import static javax.measure.unit.SI.*;

public class ConversionBot extends DiscordBot implements TextListener {

  private static final Pattern IS_UNIT = Pattern.compile("(-?[0-9]*[.,]?[0-9]+)\\s?([a-zA-Z]{1,})");
  // Length units
  private static final UnitConverter fromInch = INCH.getConverterTo(CENTIMETER); // in
  private static final UnitConverter fromFoot = FOOT.getConverterTo(CENTIMETER); // ft
  private static final UnitConverter fromYard = YARD.getConverterTo(CENTIMETER); // yd
  private static final UnitConverter fromMile = MILE.getConverterTo(CENTIMETER); // mi
  //Mass units
  private static final UnitConverter fromOunce = OUNCE.getConverterTo(GRAM); // oz
  private static final UnitConverter fromPound = POUND.getConverterTo(GRAM); /// lb
  //Volume units
  private static final UnitConverter fromGallon = GALLON_UK.getConverterTo(LITER); //ga
  //Temperature units
  private static final UnitConverter fromFarenheit = FAHRENHEIT.getConverterTo(CELSIUS); //fa
  private User SatanName;

  @Override
  public void prepare(JDA jda, Config cfg) {
    super.prepare(jda, cfg);
    SatanName = jda.getSelfUser();
  }

  @Override
  public boolean parseMessage(Message message) {
    // TODO: use less code
    if (message.getAuthor() == SatanName)
      return false;
    String result;
    boolean replySent = false;
    Matcher matches = IS_UNIT.matcher(message.getContent());
    while(matches.find()) {
      double x = Double.parseDouble(matches.group(1).replace(',','.'));
      if(matches.group(2).length() == 1)
        result = buildResponse(x,matches.group(2).substring(0,1));
      else
        result = buildResponse(x,matches.group(2).substring(0,2));
      if (result != null) {
        reply(message, "%s in non-retarded units" ,result).now();
        replySent = true;
      }
    }
    return replySent;
  }

  private String buildResponse(double number, String unit){
    double x;
    String s = "FYI " + String.valueOf(number) + " " +  unit + " is ";
    if (number <= 0 && (unit != "fa" || unit != "F"))
      return null;
    switch(unit){
      case "in":
        x = fromInch.convert(Measure.valueOf(number, INCH).doubleValue(INCH));
        return s + upLength(x);
      case "ft":
        x = fromFoot.convert(Measure.valueOf(number, FOOT).doubleValue(FOOT));
        return s + upLength(x);
      case "yd":
        x = fromYard.convert(Measure.valueOf(number, YARD).doubleValue(YARD));
        return s + upLength(x);
      case "mi":
        x = fromMile.convert(Measure.valueOf(number, MILE).doubleValue(MILE));
        return s + upLength(x);
      case "oz":
        x = fromOunce.convert(Measure.valueOf(number, OUNCE).doubleValue(OUNCE));
        return s + upMass(x);
      case "lb":
        x = fromPound.convert(Measure.valueOf(number, POUND).doubleValue(POUND));
        return s + upMass(x);
      case "ga":
        x = fromGallon.convert(Measure.valueOf(number, GALLON_UK).doubleValue(GALLON_UK));
        return s + upVolume(x);
      case "F":
      case "fa":
        x = fromFarenheit.convert(Measure.valueOf(number, FAHRENHEIT).doubleValue(FAHRENHEIT));
        return s + upTemperature(x);
    }
    return null;
  }

  private String upMass (double x){
    if (x >= 1000) {
      x = Math.round(x / 10);
      x = x/100;
      return String.valueOf(x) + " kg";
    }
    else{
      x = Math.round(x * 100);
      x = x/100;
      return String.valueOf(x) + " g";
    }
  }

  private String upVolume (double x){
    x = Math.round(x * 100);
    x = x/100;
    return String.valueOf(x) + " l";
  }

  private String upLength (double x){
    if (x >= 100000){
      x = Math.round(x / 1000);
      x = x/100;
      return String.valueOf(x) + " km";
    }
    else
    if (x >= 100){
      x = Math.round(x);
      x = x/100;
      return String.valueOf(x) + " m";
    }
    else{
      x = Math.round(x * 100);
      x = x/100;
      return String.valueOf(x) + " cm";
    }
  }

  private String upTemperature (double x) {
    x = Math.round(x * 100);
    x = x/100;
    return String.valueOf(x) + " C";
  }


}

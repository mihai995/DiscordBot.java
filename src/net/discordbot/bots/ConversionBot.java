package net.discordbot.bots;

import com.google.common.collect.ImmutableMultimap;
import net.discordbot.common.BasicCommand;
import net.discordbot.common.DiscordBot;
import net.discordbot.common.TextListener;
import net.discordbot.util.Config;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import org.jscience.physics.amount.Amount;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import static javax.measure.unit.NonSI.*;
import static javax.measure.unit.SI.*;

public class ConversionBot extends DiscordBot implements TextListener {

  private static final String NUMBER = "-?[0-9]*[.,]?[0-9]+";

  private static final String UNIT = "[a-zA-Z]*[.a-zA-Z]?[a-zA-Z]+";

  private static final Pattern IS_UNIT =
      Pattern.compile(String.format("(%s)\\s*(%s)", NUMBER, UNIT));

  private static final String CONVERSION_FORMAT = "FYI %s is %.2f%s in non-retarded units";

  private User satanName;

  private static final ImmutableMultimap<Unit, Unit> UNIT_PREFERENCES =
      ImmutableMultimap.<Unit, Unit>builder()
          .putAll(KILOGRAM, KILOGRAM, GRAM, METRIC_TON, MICRO(GRAM), NANO(GRAM))
          .putAll(METER, METER, CENTIMETER, KILOMETER)
          .putAll(CUBIC_METRE, LITER, MILLI(LITER))
          .put(KELVIN, CELSIUS)
          .build();

  @Override
  public void prepare(JDA jda, Config cfg) {
    super.prepare(jda, cfg);
    satanName = jda.getSelfUser();
  }

  @Override
  public boolean parseMessage(Message message) {
    if (message.getAuthor() == satanName) {
      return false;
    }
    Matcher matcher = IS_UNIT.matcher(message.getContent());
    boolean processed = false;
    while (matcher.find()) {
      processed |= convert(message, matcher.group(0));
    }
    return processed;
  }

  @BasicCommand("converts imperial units to metric units")
  public boolean convert(Message msg, String quantity) {
    quantity = quantity.replace(',', '.');
    Amount amount;
    try {
      amount = Amount.valueOf(quantity);
    } catch (IllegalArgumentException e) {
      return false;
    }
    Unit oldUnit = amount.getUnit();

    Unit unit = amount.getUnit().getStandardUnit();
    Collection<Unit> possibleUnits = UNIT_PREFERENCES.get(unit);

    if (unit.equals(oldUnit) || possibleUnits.contains(oldUnit)) {
      // Unit is already one of the preferred ones or there are no defined preferences. Do nothing.
      return false;
    }
    if (possibleUnits.isEmpty()) {
      possibleUnits = List.of(unit);
    }

    amount = getPreferredAmount(amount, possibleUnits);
    reply(msg, CONVERSION_FORMAT, quantity, amount.getEstimatedValue(), amount.getUnit()).soon();
    return true;
  }

  /** Converts the `amount` to the best suited unit in `possibleUnits`. */
  private static Amount getPreferredAmount(Amount amount, Collection<Unit> possibleUnits) {
    double best = Double.POSITIVE_INFINITY;
    for (Unit unit : possibleUnits) {
      Amount option = amount.to(unit);
      double current = score(option);
      if (current < best) {
        amount = option;
        best = current;
      }
    }
    return amount;
  }

  /**
   * Returns a score that indicates how well the `amount` is characterized by its unit of measure.
   * The lower the score is the stronger the preference.
   */
  private static double score(Amount amount) {
    return Math.abs(1 - Math.log10(1e-20 + Math.abs(amount.getEstimatedValue())));
  }

  static {
    alias(INCH, "in", "inch", "inches");
    alias(FOOT, "ft", "foot", "feet");
    alias(YARD, "yd", "yard", "yards");
    alias(MILE, "mi", "mile", "miles", "mila");
    alias(OUNCE, "oz", "ounce", "ounces", "uncie", "uncii");
    alias(OUNCE_LIQUID_US, "fl.oz", "floz");
    alias(POUND, "lb", "lbs", "pound", "pounds");
    alias(POUND.times(14), "stone", "stones");
    alias(FAHRENHEIT, "F", "Fa", "Fahrenheit");
    alias(GALLON_LIQUID_US, "ga", "gallon", "gallons");
    alias(GALLON_LIQUID_US.divide(8), "pint", "pints");
  }

  private static void alias(Unit unit, String... aliases) {
    UnitFormat units = UnitFormat.getInstance();
    for (String alias : aliases) {
      units.alias(unit, alias);
    }
  }
}

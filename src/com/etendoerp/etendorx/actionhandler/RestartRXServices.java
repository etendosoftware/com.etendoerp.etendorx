package com.etendoerp.etendorx.actionhandler;

import java.io.IOException;
import java.net.ConnectException;
import java.util.StringJoiner;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.etendoerp.etendorx.data.ETRXConfig;
import com.etendoerp.etendorx.utils.RXServiceManagementUtils;
import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Action that checks the current state of RX services and attempts to restart them.
 * <p>
 * Behavior per service:
 * <ul>
 *   <li>If configuration is invalid (null service or URL), records an ERROR.</li>
 *   <li>If the service name is "config", it will not be restarted; records a WARNING.</li>
 *   <li>Otherwise, checks if the service is running and performs a restart using the actuator endpoint.</li>
 * </ul>
 * The global result type is aggregated from per-service outcomes:
 * <pre>
 *   - ERROR + (SUCCESS or WARNING) -> WARNING
 *   - only ERROR                   -> ERROR
 *   - only WARNING                 -> WARNING
 *   - only SUCCESS                 -> SUCCESS
 * </pre>
 */
public class RestartRXServices extends Action {

  /**
   * Logger for this class.
   */
  private static final Logger log = LogManager.getLogger();

  /**
   * Relative path to the Spring Boot actuator restart endpoint.
   */
  private static final String ACTUATOR_RESTART = "/actuator/restart";

  /**
   * Normalizes nullable or blank messages to an empty, trimmed string-safe value.
   *
   * @param msg
   *     the original message (maybe null/blank)
   * @return a non-null string (possibly empty), trimmed if not blank
   */
  private static String safeMsg(String msg) {
    return (StringUtils.isBlank(msg)) ? "" : msg.trim();
  }

  /**
   * Checks if the given service is the special "config" service (which must not be restarted).
   *
   * @param s
   *     the service (may be null)
   * @return {@code true} if the service name equals "config" (case-insensitive), {@code false} otherwise
   */
  private static boolean isConfig(ETRXConfig s) {
    final String name = (s != null) ? s.getServiceName() : null;
    return "config".equalsIgnoreCase(name);
  }

  /**
   * Returns an uppercase display name for the service, or {@code "<UNKNOWN>"} if not available.
   *
   * @param s
   *     the service (maybe null)
   * @return uppercase service name or {@code "<UNKNOWN>"}
   */
  private static String displayNameOf(ETRXConfig s) {
    final String name = (s != null) ? s.getServiceName() : null;
    return (name != null) ? name.toUpperCase() : "<UNKNOWN>";
  }

  /**
   * Converts the given string so that only the first character is uppercase
   * and the rest of the characters are lowercase.
   * <p>
   * If the input is {@code null} or empty, it is returned unchanged.
   * </p>
   *
   * @param input
   *     the string to format
   * @return the formatted string with the first letter in uppercase and the rest in lowercase,
   *     or the original value if {@code input} is {@code null} or empty
   */
  private static String capitalizeFirstLetter(String input) {
    if (StringUtils.isBlank(input)) {
      return input;
    }
    String lower = input.toLowerCase();
    return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
  }

  /**
   * Orchestrates the restart operation for all input services.
   * <p>
   * For each service, delegates to {@link #handleService(ETRXConfig, StringJoiner)} and aggregates the
   * final outcome using {@link #aggregateOutcome(boolean, boolean, boolean)}.
   *
   * @param parameters
   *     optional JSON parameters (unused)
   * @param isStopped
   *     cooperative stop flag (unused in this implementation; loop is short-lived)
   * @return the aggregated {@link ActionResult} containing a final {@link Result.Type} and a per-service message list
   */
  @Override
  protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
    final ActionResult global = new ActionResult();
    final StringJoiner messages = new StringJoiner(System.lineSeparator());

    boolean sawSuccess = false;
    boolean sawWarning = false;
    boolean sawError = false;

    final var input = getInputContents(getInputClass());

    for (ETRXConfig service : input) {
      Result.Type outcome = handleService(service, messages);
      if (outcome == Result.Type.ERROR) {
        sawError = true;
      } else if (outcome == Result.Type.WARNING) {
        sawWarning = true;
      } else if (outcome == Result.Type.SUCCESS) {
        sawSuccess = true;
      }
    }

    global.setType(aggregateOutcome(sawSuccess, sawWarning, sawError));
    global.setMessage(messages.toString());
    return global;
  }

  /**
   * Handles a single service end-to-end: validates input, prevents restart for "config",
   * and invokes {@link #restartService(ETRXConfig, String, StringJoiner)} for normal cases.
   * Adds exactly one line to {@code messages} and returns the local outcome.
   *
   * @param service
   *     the service to process
   * @param messages
   *     accumulator for per-service messages
   * @return the local {@link Result.Type} for the processed service
   */
  private Result.Type handleService(ETRXConfig service, StringJoiner messages) {
    final String dn = capitalizeFirstLetter(displayNameOf(service));

    if (service == null || StringUtils.isBlank(service.getServiceURL())) {
      messages.add(String.format(OBMessageUtils.messageBD("ETRX_NullServiceOrURL"), dn));
      return Result.Type.ERROR;
    }
    if (isConfig(service)) {
      final String msg = OBMessageUtils.messageBD("ETRX_ConfigCanNotRestart");
      log.warn(msg);
      messages.add(msg);
      return Result.Type.WARNING;
    }

    return restartService(service, dn, messages);
  }

  /**
   * Performs "check running" and, if OK, attempts to restart the service via actuator.
   * Handles exceptions and translates all outcomes to a single message line and a {@link Result.Type}.
   *
   * @param service
   *     the target service (non-null, validated by caller)
   * @param dn
   *     precomputed display name (non-null)
   * @param messages
   *     accumulator for per-service messages
   * @return the local {@link Result.Type} inferred from the operation
   */
  private Result.Type restartService(ETRXConfig service, String dn, StringJoiner messages) {
    final ActionResult local = new ActionResult();
    local.setType(Result.Type.SUCCESS);

    try {
      RXServiceManagementUtils.checkRunning(service.getServiceURL(), local);
      if (local.getType() == Result.Type.ERROR) {
        messages.add(dn + " " + safeMsg(local.getMessage()));
        return Result.Type.ERROR;
      }

      RXServiceManagementUtils.performRestart(service.getServiceURL() + ACTUATOR_RESTART, local);
      return interpretLocal(local, dn, messages);

    } catch (ConnectException ce) {
      messages.add(String.format(OBMessageUtils.messageBD("ETRX_ServiceNotRunningYet"), dn));
      return Result.Type.WARNING;
    } catch (IOException ioe) {
      log.error("Error while restarting RX services", ioe);
      messages.add(String.format(OBMessageUtils.messageBD("ETRX_RestartFailed"), dn, safeMsg(ioe.getMessage())));
      return Result.Type.ERROR;
    } catch (RuntimeException re) {
      log.error("Unexpected error while restarting {}", dn, re);
      messages.add(String.format(OBMessageUtils.messageBD("ETRX_UnexpectedError"), dn, safeMsg(re.getMessage())));
      return Result.Type.ERROR;
    }
  }

  /**
   * Interprets the {@link ActionResult} returned by the underlying utils into a final message and type.
   *
   * @param local
   *     the local action result populated by utility calls
   * @param dn
   *     display name for the service
   * @param messages
   *     accumulator for per-service messages
   * @return {@link Result.Type#ERROR}, {@link Result.Type#WARNING}, or {@link Result.Type#SUCCESS}
   */
  private Result.Type interpretLocal(ActionResult local, String dn, StringJoiner messages) {
    if (local.getType() == Result.Type.ERROR) {
      messages.add(dn + " " + safeMsg(local.getMessage()));
      return Result.Type.ERROR;
    }
    if (local.getType() == Result.Type.WARNING) {
      messages.add(dn + " " + safeMsg(local.getMessage()));
      return Result.Type.WARNING;
    }
    messages.add(String.format(OBMessageUtils.messageBD("ETRX_ServiceRestarted"), dn));
    return Result.Type.SUCCESS;
  }

  /**
   * Aggregates the overall outcome from the individual flags collected across services.
   *
   * @param success
   *     whether any service succeeded
   * @param warning
   *     whether any service produced a warning
   * @param error
   *     whether any service failed
   * @return the final aggregated {@link Result.Type}
   */
  private Result.Type aggregateOutcome(boolean success, boolean warning, boolean error) {
    if (error && (success || warning)) return Result.Type.WARNING;
    if (error) return Result.Type.ERROR;
    if (warning) return Result.Type.WARNING;
    return Result.Type.SUCCESS;
  }

  /**
   * Declares the input entity class for this action.
   *
   * @return the input class type consumed by {@link #getInputContents(Class)}
   */
  @Override
  protected Class<ETRXConfig> getInputClass() {
    return ETRXConfig.class;
  }
}

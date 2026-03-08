from __future__ import annotations

import re
import shutil
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "aiknowledge"

MODULES = {
	"YadaWeb": {
		"source": ROOT / "YadaWeb" / "src" / "main" / "java",
		"entry": "yadaWeb_dictionary.md",
		"folder": "yadaWeb",
		"title": "YadaWeb",
		"summary": "Core web, persistence, utility, and DataTables APIs.",
	},
	"YadaWebSecurity": {
		"source": ROOT / "YadaWebSecurity" / "src" / "main" / "java",
		"entry": "yadaWebSecurity_dictionary.md",
		"folder": "yadaWebSecurity",
		"title": "YadaWebSecurity",
		"summary": "Security helpers, controllers, and persistence APIs.",
	},
	"YadaWebCMS": {
		"source": ROOT / "YadaWebCMS" / "src" / "main" / "java",
		"entry": "yadaWebCMS_dictionary.md",
		"folder": "yadaWebCMS",
		"title": "YadaWebCMS",
		"summary": "CMS-specific helpers and persistence APIs.",
	},
	"YadaAi": {
		"source": ROOT / "YadaAi" / "src" / "main" / "java",
		"entry": "yadaAI_dictionary.md",
		"folder": "yadaAI",
		"title": "YadaAI",
		"summary": "Bedrock Claude request builders and AI utility helpers.",
	},
}

TYPE_SIG_RE = re.compile(r"^\s*(?:public\s+)?(?:abstract\s+|final\s+)?(class|interface|enum)\s+(\w+)\s*([^\{]*)\{")
METHOD_SIG_RE = re.compile(r"^\s*public\s+(?!class\b|interface\b|enum\b)(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?(?:abstract\s+)?(?:native\s+)?(?:<[^>]+>\s*)?[\w<>\[\], ?.&@]+?\s+(\w+)\s*\(([^)]*)\)\s*(?:throws [^{;]+)?[;{]")
PACKAGE_RE = re.compile(r"^\s*package\s+([\w.]+);", re.MULTILINE)

SKIP_METHODS = {"main", "init", "equals", "hashCode", "iterator", "toString"}
EXCLUDED_PACKAGES = {"net.yadaframework.web.dialect", "org.springframework.web.multipart.commons"}
EXCLUDED_CLASSES = {
	"YadaAjaxInterceptor", "YadaAiConfig", "YadaAiConfigImportSelector", "YadaAppConfig", "YadaAttachedFileCloneSet",
	"YadaCmsConfig", "YadaCmsConfiguration", "YadaCmsProductController", "YadaController", "YadaCropDefinition",
	"YadaDataTableFactory", "YadaDataTableHelper", "YadaDataTableButtonProxy", "YadaDataTableColumnProxy",
	"YadaDataTableConfirmDialogProxy", "YadaDataTableHTMLProxy", "YadaDataTableLanguageProxy", "YadaDataTableProxy",
	"YadaDbStatsLoggingConfigurer", "YadaDialectUtil", "YadaDTColumnDefProxy", "YadaDTColumnsProxy", "YadaDTOptionsProxy",
	"YadaDummyDatasource", "YadaDummyEntityManagerFactory", "YadaDummyJpaConfig", "YadaFacebookRequest", "YadaFacebookRequestV9",
	"YadaFlotAxis", "YadaFlotChart", "YadaFlotGrid", "YadaFlotPlotOptions", "YadaFlotSeriesObject", "YadaFluentBase",
	"YadaFormFieldMap", "YadaFormHelper", "YadaFormPasswordChange", "YadaGallerySlide", "YadaGlobalExceptionHandler",
	"YadaHibernateStatsLogger", "YadaJobScheduler", "YadaJsonMapper", "YadaJsonView", "YadaLinkBuilder",
	"YadaJpaConfig", "YadaLocalePathLinkBuilder", "YadaLocalePathRequestCache", "YadaLoginController", "YadaMariaDB", "YadaMiscController",
	"YadaPageRows", "YadaPageSort", "YadaPersistentEnumEditor", "YadaSecurityBeans", "YadaSecurityConfig",
	"YadaSecurityUtilStub", "YadaSession", "YadaSetup", "YadaSocial", "YadaSocialRegistrationData", "YadaTomcatPoolMetricsLogger",
	"YadaTomcatServer", "YadaTraceStatementInspector", "YadaWebApplicationInitializer", "YadaWebConfig", "YadaWebSecurityConfig",
	"YadaWrappedSavedRequest", "UCheckDigit", "UCheckNum", "YadaDialectWithSecurity", "YadaJobState", "YadaTicketStatus", "YadaTicketType",
	"YadaUserMessageType",
}
INFRA_TOKENS = [
	"handlerinterceptor", "webmvcconfigurer", "importselector", "formatter<", "attributeconverter<", "jsonserializer<",
	"propertyeditorsupport", "statementinspector", "datasource", "entitymanagerfactory", "httpsessionrequestcache",
	"savedrequestawareauthenticationsuccesshandler", "authenticationfailurehandler", "authenticationhandler", "authenticationprovider",
	"filter", "genericfilterbean", "onceperrequestfilter", "simpleurllogoutsuccesshandler", "userdetailsservice",
	"standardservletmultipartresolver", "abstractannotationconfigdispatcherservletinitializer", "abstractsecuritywebapplicationinitializer",
	"thymeleaf",
]

KEY_CLASS_DESCRIPTIONS = {
	"YadaAiUtil": "Collects helper methods for localized prompts and Claude Bedrock invocation. Use when Yada code needs one-call AI request execution.",
	"YadaAttachedFile": "Represents a persisted attachment and the operations needed to place, move, rename, and order its physical files. Use as the model for Yada-managed uploads.",
	"YadaConfiguration": "Exposes framework configuration values resolved from properties and setup data. Use when code needs environment-specific Yada settings.",
	"YadaDataTable": "Builds the root server-side DataTables configuration object. Use as the entry point for Yada's fluent DataTables API.",
	"YadaDataTableDao": "Executes paged queries and converts the results to DataTables JSON payloads. Use behind server-side Yada DataTable endpoints.",
	"YadaEmailBuilder": "Builds templated email payloads with recipients, model data, inline resources, and attachments. Use before handing the message to YadaEmailService.",
	"YadaEmailService": "Loads Thymeleaf mail templates and sends HTML or text emails. Use when application code needs links, batching, and Yada mail conventions.",
	"YadaFileManager": "Keeps attached files, derived image variants, and temporary uploads consistent on disk and in the database. Use for upload, clone, move, rename, and cleanup flows.",
	"YadaHttpUtil": "Provides HTTP-oriented helpers for cookies, redirects, compressed payloads, and URL parsing. Use when working below the MVC layer.",
	"YadaIntDimension": "Represents image or rectangle dimensions with comparison helpers. Use when sizing, cropping, or comparing media bounds.",
	"YadaJobManager": "Starts, pauses, resumes, and completes persistent background jobs tracked in the database. Use when long-running work must survive requests and expose state.",
	"YadaKeyRateLimiter": "Applies per-key rate limiting backed by logged timestamps. Use when requests must be throttled independently for each identifier.",
	"YadaLongRunningExclusive": "Serializes long-running work so only one invocation with the same lock can execute at a time. Use around expensive tasks that must not overlap.",
	"YadaLookupTable": "Stores values behind a single-key lookup without repeated map boilerplate. Use when one computed key should resolve to one cached value.",
	"YadaLookupTableThree": "Stores values behind a three-key lookup without nested map boilerplate. Use when combinations of three keys need fast retrieval.",
	"YadaLookupTableFour": "Stores values behind a four-key lookup without nested map boilerplate. Use when combinations of four keys need fast retrieval.",
	"YadaLookupTableFive": "Stores values behind a five-key lookup without nested map boilerplate. Use when combinations of five keys need fast retrieval.",
	"YadaLookupTableSix": "Stores values behind a six-key lookup without nested map boilerplate. Use when combinations of six keys need fast retrieval.",
	"YadaMoney": "Wraps money amounts as integer minor units and exposes safe arithmetic helpers. Use instead of floating-point math for prices and totals.",
	"YadaNetworkUtil": "Finds the machine's public network addresses. Use in diagnostics or environment discovery.",
	"YadaNotify": "Builds notification-modal responses and page actions returned to the browser. Use from controllers when Ajax flows must show feedback or trigger client behavior.",
	"YadaNotifyData": "Stores one notification entry inside a Yada modal response. Use through YadaNotify to add messages, redirects, or follow-up actions.",
	"YadaPublicSuffix": "Parses public-suffix data to split hostnames into registrable domains and subdomains. Use when cookies or multi-tenant domains depend on the effective top-level domain.",
	"YadaRegexUtil": "Caches patterns and applies regional regex extraction and replacement helpers. Use when repeated regex work needs better performance or bounded replacements.",
	"YadaRegistrationController": "Exposes registration, password reset, and username change endpoints. Reuse or subclass it when enabling the built-in registration flows.",
	"YadaSecurityEmailService": "Builds security-specific emails such as registration, password reset, and email change confirmations. Use when authentication flows must send signed links.",
	"YadaSecurityUtil": "Collects higher-level security helpers for access checks, password changes, login state, and controller error parameters. Use from security-aware services and controllers.",
	"YadaSeleniumUtil": "Wraps common Selenium actions and wait patterns used in Yada browser tests. Use in test code to write shorter, more reliable browser interactions.",
	"YadaSql": "Builds SQL or JPQL fragments incrementally with conditional joins, filters, grouping, ordering, unions, and parameters. Use when dynamic queries are clearer than Criteria or manual string concatenation.",
	"YadaTokenHandler": "Creates and parses signed autologin and one-time security links. Use when flows need expiring links tied to a user and purpose.",
	"YadaUserCredentials": "Represents login credentials, roles, and linked social accounts for a user. Use as the persisted authentication record.",
	"YadaUserMessage": "Represents a user-visible message or ticket message with hashing, stack, attachment, and relative-time helpers. Use as the base model for in-app messages.",
	"YadaUserProfile": "Represents the application profile attached to a user account. Use when business data must be linked to credentials and role checks.",
	"YadaUtil": "Collects low-level helpers for strings, dates, files, JSON, reflection, localization, shell calls, and entity cloning. Use when Yada code needs reusable non-web utilities.",
	"YadaWebUtil": "Collects web-layer helpers for URLs, redirects, uploads, downloads, cookies, fragments, modals, and request inspection. Use from controllers, views, and web support code.",
	"YadaSimpleRateLimiter": "Enforces a fixed request rate without sleeping the caller. Use when you need a fast pass-or-fail throttle.",
	"YadaSleepingRateLimiter": "Enforces a fixed request rate by delaying the caller until it is safe to continue. Use when work should be smoothed instead of rejected.",
	"YadaClaudeRequest": "Builds an Anthropic Claude request for AWS Bedrock, including messages, system prompts, tools, and generation settings. Use before serializing and submitting a chat request.",
}

METHOD_OVERRIDES = {
	("YadaEmailBuilder", "instance"): "Creates a new email builder.",
	("YadaNotify", "instance"): "Creates a new notification builder.",
	("YadaNotify", "empty"): "Creates an empty notification response.",
	("YadaNotifyData", "add"): "Appends the notification to the current response.",
	("YadaDataTable", "dtOptionsObj"): "Adds the client-side options section.",
	("YadaPageRequest", "of"): "Creates a page request from offset and size.",
	("YadaAttachedFile", "orderBefore"): "Swaps sort order so this file comes before another one.",
	("YadaAttachedFile", "calcAndSetTargetFile"): "Computes and stores the target file path for a derived file variant.",
	("YadaConfiguration", "getForB3B4B5"): "Returns the value that matches the configured Bootstrap version.",
	("YadaConfiguration", "getPasswordResetSent"): "Returns the redirect URL used after sending a password-reset email.",
	("YadaConfiguration", "getRegistrationConfirmationLink"): "Returns the registration-confirmation link with locale handling.",
	("YadaConfiguration", "getCountryForLanguage"): "Returns the configured country for a language.",
	("YadaConfiguration", "getRoleSpringName"): "Returns the Spring Security role name for a configured role ID.",
	("YadaConfiguration", "getRoleKey"): "Returns the message key for a configured role ID.",
	("YadaConfiguration", "containsInProperties"): "Returns the subset of properties whose keys contain the search text.",
	("YadaConfiguration", "encodePassword"): "Returns whether password encoding is enabled.",
	("YadaConfiguration", "seleniumWaitQuick"): "Returns the short Selenium wait timeout.",
	("YadaConfiguration", "seleniumWait"): "Returns the default Selenium wait timeout.",
	("YadaConfiguration", "seleniumWaitSlow"): "Returns the long Selenium wait timeout.",
	("YadaEmailService", "sendSupportRequest"): "Sends the configured support-request email.",
	("YadaEmailService", "sendEmailBatch"): "Sends a batch of prepared emails.",
	("YadaEmailService", "timestamp"): "Returns a formatted timestamp string.",
	("YadaHttpUtil", "redirectPermanent"): "Sends a permanent HTTP redirect.",
	("YadaHttpUtil", "redirectTemporary"): "Sends a temporary HTTP redirect.",
	("YadaHttpUtil", "forward"): "Forwards the request to another page.",
	("YadaJobManager", "reschedule"): "Reschedules a job to a new execution time.",
	("YadaRegistrationType", "fromInt"): "Returns the enum value stored at the given ordinal.",
	("YadaRegistrationType", "toInt"): "Returns the enum ordinal used in persistence.",
	("YadaSecurityEmailService", "sendEmailChangeConfirmation"): "Sends the email-change confirmation message.",
	("YadaSecurityUtil", "caseAnonAuth"): "Returns one of two values depending on whether the current user is authenticated.",
	("YadaSecurityUtil", "registrationRequestCleanup"): "Deletes old or duplicate registration requests for the same email and type.",
	("YadaSeleniumUtil", "positionWindow"): "Sets the browser window position and size.",
	("YadaSeleniumUtil", "waitWhileVisible"): "Waits until the element becomes invisible.",
	("YadaSleepingRateLimiter", "sleepWhenNeeded"): "Sleeps long enough to keep the configured rate.",
	("YadaSql", "instance"): "Creates an empty query builder.",
	("YadaSql", "deleteFrom"): "Starts a delete query.",
	("YadaSql", "selectFrom"): "Starts or extends a select query.",
	("YadaSql", "selectFromReplace"): "Replaces the current select section.",
	("YadaSql", "set"): "Adds assignments to the current set clause.",
	("YadaSql", "updateSet"): "Starts an update query.",
	("YadaSql", "join"): "Adds join clauses if missing.",
	("YadaSql", "type"): "Adds a JPQL TYPE condition.",
	("YadaSql", "dtype"): "Adds a native DTYPE condition.",
	("YadaSql", "where"): "Adds a where condition.",
	("YadaSql", "whereNotEmpty"): "Adds a where condition only when a collection has values.",
	("YadaSql", "whereIn"): "Adds an IN condition.",
	("YadaSql", "having"): "Adds a having condition.",
	("YadaSql", "and"): "Queues an AND operator for the next condition.",
	("YadaSql", "or"): "Queues an OR operator for the next condition.",
	("YadaSql", "xor"): "Queues an XOR operator for the next condition.",
	("YadaSql", "startSubexpression"): "Starts a grouped subexpression.",
	("YadaSql", "endSubexpression"): "Closes the current grouped subexpression.",
	("YadaSql", "groupBy"): "Adds group-by clauses.",
	("YadaSql", "orderBy"): "Adds order-by clauses.",
	("YadaSql", "orderByNative"): "Adds MySQL order-by clauses from a page request.",
	("YadaSql", "limit"): "Sets the query limit.",
	("YadaSql", "clearWhere"): "Clears all where conditions.",
	("YadaSql", "toCount"): "Transforms the query into a count query.",
	("YadaSql", "toSelectFrom"): "Replaces the current select-from section.",
	("YadaSql", "nativeQuery"): "Creates a native JPA query from the built SQL.",
	("YadaSql", "query"): "Creates a JPQL query from the built SQL.",
	("YadaSql", "setParameter"): "Stores a named parameter for later query creation.",
	("YadaSql", "add"): "Merges joins, filters, grouping, ordering, and parameters from another query.",
	("YadaSql", "sql"): "Returns the built SQL string.",
	("YadaSql", "overwriteQuery"): "Replaces the whole query text.",
	("YadaSql", "getWhere"): "Returns the current where fragment without the prefix.",
	("YadaUtil", "compare"): "Compares nullable values.",
	("YadaUtil", "getJsonObject"): "Returns a nested JSON object at a path.",
	("YadaUtil", "getJsonArray"): "Returns a nested JSON array at a path.",
	("YadaUtil", "findGenericClass"): "Finds the concrete class bound to a generic superclass.",
	("YadaUtil", "autowire"): "Autowires an object that was created outside the Spring context.",
	("YadaUtil", "autowireAndInitialize"): "Autowires and initializes an object created outside the Spring context.",
	("YadaUtil", "sleepRandom"): "Sleeps for a random duration inside the given bounds.",
	("YadaUtil", "sleep"): "Sleeps for the given number of milliseconds.",
	("YadaUtil", "minutesAgo"): "Returns a timestamp a given number of minutes in the past.",
	("YadaUtil", "daysAgo"): "Returns a timestamp a given number of days in the past.",
	("YadaUtil", "shellExec"): "Runs an external command configured in Yada settings.",
	("YadaUtil", "splitAtWord"): "Splits a string near the requested position without breaking the last word.",
	("YadaUtil", "normalizzaCellulareItaliano"): "Normalizes an Italian mobile number.",
	("YadaUtil", "validaCellulare"): "Validates an Italian mobile number.",
	("YadaWebUtil", "isEmpty"): "Checks whether a paged row wrapper is null or empty.",
	("YadaWebUtil", "passThrough"): "Copies request parameters into the model.",
	("YadaWebUtil", "compare"): "Compares values with null-safety.",
	("YadaWebUtil", "cleanContent"): "Cleans HTML content while allowing extra tags.",
	("YadaWebUtil", "ensureThymeleafUrl"): "Prefixes a plain URL so Thymeleaf treats it as a link expression.",
	("YadaWebUtil", "registerDynamicMapping"): "Registers a request mapping dynamically at runtime.",
	("YadaClaudeMessage", "roleUser"): "Sets the message role to user.",
	("YadaClaudeMessage", "roleAssistant"): "Sets the message role to assistant.",
	("YadaUserMessage", "computeHash"): "Computes the message hash used to detect duplicates.",
	("YadaUserMessage", "incrementStack"): "Increments the stack counter and stores the new timestamp.",
	("YadaUserMessageDao", "markAsRead"): "Marks the given messages as read.",
	("YadaUserProfileDao", "updateTimezone"): "Updates a user's timezone by username.",
}

WORD_MAP = {"Ajax": "AJAX", "Api": "API", "Css": "CSS", "Db": "DB", "Html": "HTML", "Ip": "IP", "Iso": "ISO", "Id": "ID", "Jpa": "JPA", "Jpql": "JPQL", "Json": "JSON", "Pdf": "PDF", "Rfc": "RFC", "Sql": "SQL", "Url": "URL", "Obj": ""}
ITALIAN_MARKERS = {"aggiunge", "gestisce", "questo", "questa", "utilita", "utilita'", "restituisce", "ritorna", "filtrate", "configurato", "usato", "usare"}

@dataclass
class MethodInfo:
	name: str
	params: str
	annotations: list[str] = field(default_factory=list)
	javadoc: str | None = None


@dataclass
class TypeInfo:
	module_key: str
	package: str
	name: str
	kind: str
	tail: str
	annotations: list[str] = field(default_factory=list)
	javadoc: str | None = None
	methods: list[MethodInfo] = field(default_factory=list)

	@property
	def full_name(self) -> str:
		return f"{self.package}.{self.name}"
def clean_javadoc(doc: str | None) -> str | None:
	if not doc:
		return None
	text = re.sub(r"^\s*/\*\*", "", doc)
	text = re.sub(r"\*/\s*$", "", text)
	text = re.sub(r"(?m)^\s*\*\s?", "", text)
	text = text.replace("{@link", "").replace("{@code", "").replace("}", "")
	text = re.sub(r"<[^>]+>", "", text)
	text = re.sub(r"\s+", " ", text).strip()
	return text or None


def strip_inline_noise(line: str) -> str:
	line = re.sub(r"//.*", "", line)
	line = re.sub(r"/\*.*?\*/", "", line)
	line = re.sub(r'"(?:\\.|[^"\\])*"', '""', line)
	line = re.sub(r"'(?:\\.|[^'\\])*'", "''", line)
	return line


def split_words(name: str) -> list[str]:
	if name.startswith("dt") and len(name) > 2 and name[2].isupper():
		name = name[2:]
	if name.startswith("yada") and len(name) > 4 and name[4].isupper():
		name = name[4:]
	return re.findall(r"[A-Z]+(?=[A-Z][a-z]|$)|[A-Z]?[a-z]+|[0-9]+", name) or [name]


def normalize_words(words: list[str]) -> list[str]:
	return [WORD_MAP.get(word, word) for word in words if WORD_MAP.get(word, word)]


def phrase_from_name(name: str) -> str:
	words = normalize_words(split_words(name))
	return " ".join(word if word.isupper() or word.isdigit() else word.lower() for word in words)


def first_sentence(doc: str | None) -> str | None:
	cleaned = clean_javadoc(doc)
	if not cleaned:
		return None
	cleaned = cleaned.split("@param", 1)[0].split("@return", 1)[0].split("@see", 1)[0].strip()
	if not cleaned or any(marker in cleaned.lower() for marker in ITALIAN_MARKERS):
		return None
	match = re.search(r"[.!?](?:\s|$)", cleaned)
	return cleaned[: match.end()].strip() if match else cleaned


def parse_java_file(module_key: str, path: Path) -> TypeInfo | None:
	raw = path.read_text(encoding="utf-8")
	package_match = PACKAGE_RE.search(raw)
	if not package_match:
		return None
	pending_doc = None
	pending_annotations: list[str] = []
	comment_buffer: list[str] = []
	in_block_comment = False
	comment_is_javadoc = False
	depth = 0
	type_info: TypeInfo | None = None
	for line in raw.splitlines():
		stripped = line.strip()
		if in_block_comment:
			if comment_is_javadoc:
				comment_buffer.append(line)
			if "*/" in line:
				if comment_is_javadoc:
					pending_doc = clean_javadoc("\n".join(comment_buffer))
				in_block_comment = False
				comment_is_javadoc = False
				comment_buffer = []
			continue
		if stripped.startswith("/**"):
			in_block_comment = True
			comment_is_javadoc = True
			comment_buffer = [line]
			if "*/" in stripped and stripped != "/**":
				pending_doc = clean_javadoc("\n".join(comment_buffer))
				in_block_comment = False
				comment_is_javadoc = False
				comment_buffer = []
			continue
		if stripped.startswith("/*"):
			in_block_comment = True
			comment_is_javadoc = False
			if "*/" in stripped and stripped != "/*":
				in_block_comment = False
			continue
		annotation_match = re.match(r"^\s*@([\w.]+)", line)
		if annotation_match and depth in {0, 1}:
			pending_annotations.append(annotation_match.group(1).split(".")[-1])
			continue
		if type_info is None:
			type_match = TYPE_SIG_RE.match(line)
			if type_match:
				type_info = TypeInfo(module_key, package_match.group(1), type_match.group(2), type_match.group(1), type_match.group(3).strip(), pending_annotations[:], pending_doc)
				pending_doc = None
				pending_annotations = []
			elif stripped and not stripped.startswith("//"):
				pending_doc = None
				pending_annotations = []
		elif depth == 1:
			method_match = METHOD_SIG_RE.match(line)
			if method_match:
				type_info.methods.append(MethodInfo(method_match.group(1), method_match.group(2).strip(), pending_annotations[:], pending_doc))
				pending_doc = None
				pending_annotations = []
			elif stripped and not stripped.startswith("//"):
				pending_doc = None
				pending_annotations = []
		depth += strip_inline_noise(line).count("{") - strip_inline_noise(line).count("}")
	return type_info


def is_deprecated(annotations: list[str], javadoc: str | None) -> bool:
	return "Deprecated" in annotations or "@deprecated" in (javadoc or "").lower()


def keep_type(info: TypeInfo) -> bool:
	if info.kind == "interface":
		return False
	if info.package in EXCLUDED_PACKAGES or info.package.startswith("org.springframework"):
		return False
	if info.name in EXCLUDED_CLASSES or is_deprecated(info.annotations, info.javadoc):
		return False
	return not any(token in info.tail.lower() for token in INFRA_TOKENS)


def keep_method(type_info: TypeInfo, method: MethodInfo) -> bool:
	if is_deprecated(method.annotations, method.javadoc):
		return False
	if method.name == type_info.name or method.name in SKIP_METHODS:
		return False
	if any(annotation in {"PostConstruct", "EventListener"} for annotation in method.annotations):
		return False
	if re.match(r"get[A-Z]", method.name) and not method.params:
		return False
	if re.match(r"is[A-Z]", method.name) and not method.params:
		return False
	if re.match(r"set[A-Z]", method.name):
		return False
	return True


def dedupe_methods(methods: list[MethodInfo]) -> list[MethodInfo]:
	result = []
	seen = set()
	for method in methods:
		if method.name in seen:
			continue
		seen.add(method.name)
		result.append(method)
	return result


def fallback_public_methods(raw: str) -> list[MethodInfo]:
	pattern = re.compile(r"(?m)^\s*public\s+(?!class\b|interface\b|enum\b)(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?(?:abstract\s+)?(?:native\s+)?(?:<[^>]+>\s*)?[\w<>\[\], ?.&@]+?\s+(\w+)\s*\(([^)]*)\)\s*(?:throws [^{;]+)?[;{]")
	return [MethodInfo(match.group(1), match.group(2).strip()) for match in pattern.finditer(raw)]


def humanize_type_name(name: str) -> str:
	return phrase_from_name(name[4:] if name.startswith("Yada") else name)


def default_class_description(info: TypeInfo) -> str:
	if info.package.endswith(".persistence.repository"):
		target = humanize_type_name(info.name.removesuffix("Dao"))
		return f"Handles persistence and query operations for {target}. Use when services need {target}-specific lookups or updates."
	if info.package.endswith(".persistence.entity"):
		target = humanize_type_name(info.name)
		return f"Represents {target} data with the small behaviors attached to that record. Use as the persisted model for {target}."
	if info.package.endswith(".web.datatables.options"):
		target = humanize_type_name(info.name)
		return f"Builds the {target} section of a DataTables configuration. Use while composing a Yada DataTable."
	if info.package.endswith(".web.datatables.config"):
		target = humanize_type_name(info.name)
		return f"Builds {target} metadata for a Yada DataTable. Use while composing the table structure and actions."
	if info.package.endswith(".ai.components.bedrock.claude.parts"):
		target = humanize_type_name(info.name)
		return f"Builds the {target} section of a Claude Bedrock request. Use while composing YadaClaudeRequest."
	if info.package.endswith(".web"):
		target = humanize_type_name(info.name)
		return f"Supports {target} behavior in the Yada web layer. Use it when that workflow must be represented or reused."
	target = humanize_type_name(info.name)
	return f"Exposes reusable {target} behavior in the Yada framework. Use it when that responsibility is needed directly."


def class_description(info: TypeInfo) -> str:
	return KEY_CLASS_DESCRIPTIONS.get(info.name, default_class_description(info))


def is_builder_context(info: TypeInfo) -> bool:
	return ".web.datatables." in info.package or ".ai.components.bedrock.claude" in info.package or info.name in {"YadaEmailBuilder", "YadaNotify", "YadaNotifyData", "YadaCropImage"}


def articleize(phrase: str) -> str:
	if not phrase or phrase.lower().startswith(("a ", "an ", "the ")):
		return phrase
	first = phrase.split(" ", 1)[0].lower()
	return ("an " if first[:1] in {"a", "e", "i", "o", "u"} else "a ") + phrase


def generic_method_description(info: TypeInfo, method: MethodInfo) -> str:
	name = method.name
	words = normalize_words(split_words(name))
	if not words:
		return "Performs the related action."
	if words[-1] == "Off":
		phrase = " ".join(word if word.isupper() else word.lower() for word in words[:-1])
		return f"Turns off {phrase}."
	if words[0] == "No":
		phrase = " ".join(word if word.isupper() else word.lower() for word in words[1:])
		return f"Disables {phrase}."
	if words[-1] == "False":
		phrase = " ".join(word if word.isupper() else word.lower() for word in words[:-1])
		return f"Disables {phrase}."
	if words[-1] == "Obj":
		phrase = " ".join(word if word.isupper() else word.lower() for word in words[:-1])
		return f"Adds {articleize(phrase)} configuration."
	if words[-1] == "Key":
		phrase = " ".join(word if word.isupper() else word.lower() for word in words[:-1])
		return f"Sets the localized {phrase} key."
	if words[-1] == "Provider":
		phrase = " ".join(word if word.isupper() else word.lower() for word in words[:-1])
		return f"Sets the {phrase} provider."
	if is_builder_context(info) and words[0].lower() not in {"add", "clear", "remove", "delete", "create", "build", "make", "load", "save", "find", "check", "has", "is", "can", "validate", "change", "copy", "move", "rename", "sort", "parse", "format", "round", "get", "set", "use", "mark", "return", "start", "end", "merge"}:
		phrase = " ".join(word if word.isupper() else word.lower() for word in words)
		return f"Sets {phrase}."
	verb = words[0].lower()
	rest = " ".join(word if word.isupper() else word.lower() for word in words[1:]).strip()
	if name == "back":
		return "Returns the parent fluent builder."
	if name == "empty":
		return "Creates an empty instance."
	if name == "instance":
		return "Creates a new instance."
	if name == "of":
		return "Creates an instance from explicit values."
	if verb == "add":
		return f"Adds {articleize(rest)}." if rest else "Adds an item."
	if verb == "remove":
		return f"Removes {articleize(rest)}." if rest else "Removes an item."
	if verb == "delete":
		return f"Deletes {articleize(rest)}." if rest else "Deletes the current item."
	if verb == "save":
		return f"Saves {articleize(rest)}." if rest else "Saves the current data."
	if verb == "load":
		return f"Loads {articleize(rest)}." if rest else "Loads the current data."
	if verb in {"build", "make", "create"}:
		return f"Builds {articleize(rest)}." if rest else "Builds a new value."
	if verb == "ensure":
		return f"Ensures {articleize(rest)}." if rest else "Ensures the required state."
	if verb == "find":
		return f"Finds {rest}." if rest else "Finds matching data."
	if verb == "count":
		return f"Counts {rest}." if rest else "Counts matching data."
	if verb == "change":
		return f"Changes {rest}." if rest else "Changes the current value."
	if verb == "copy":
		return f"Copies {rest}." if rest else "Copies the current data."
	if verb == "move":
		return f"Moves {rest}." if rest else "Moves the current item."
	if verb == "rename":
		return f"Renames {rest}." if rest else "Renames the current item."
	if verb == "sort":
		return f"Sorts {rest}." if rest else "Sorts values."
	if verb == "parse":
		return f"Parses {rest}." if rest else "Parses the current input."
	if verb == "format":
		return f"Formats {rest}." if rest else "Formats the current value."
	if verb == "round":
		return f"Rounds {rest}." if rest else "Rounds the current value."
	if verb == "prefetch":
		return f"Prefetches {rest}." if rest else "Prefetches related data."
	if verb == "clear":
		return f"Clears {rest}." if rest else "Clears the current data."
	if verb == "check":
		return f"Checks {rest}." if rest else "Checks the current state."
	if verb == "validate":
		return f"Validates {rest}." if rest else "Validates the current value."
	if verb == "has":
		return f"Checks whether it has {rest}." if rest else "Checks whether a value is present."
	if verb == "is":
		return f"Checks whether it is {rest}." if rest else "Checks the current condition."
	if verb == "can":
		return f"Checks whether it can {rest}." if rest else "Checks whether the action is allowed."
	if verb == "get":
		return f"Returns {articleize(rest)}." if rest else "Returns the current value."
	if verb == "compare":
		return "Compares values."
	return f"(maybe) Handles {phrase_from_name(name)}."


def method_description(info: TypeInfo, method: MethodInfo) -> str:
	override = METHOD_OVERRIDES.get((info.name, method.name))
	if override:
		return override
	doc_sentence = first_sentence(method.javadoc)
	if doc_sentence:
		return doc_sentence
	return generic_method_description(info, method)
def write_markdown(types_by_module: dict[str, list[TypeInfo]]) -> None:
	OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
	for module_info in MODULES.values():
		module_dir = OUTPUT_DIR / module_info["folder"]
		if module_dir.exists():
			shutil.rmtree(module_dir)
		module_dir.mkdir(parents=True, exist_ok=True)
	for filename in ["yada_dictionary.md", "yadaWeb_dictionary.md", "yadaWebSecurity_dictionary.md", "yadaWebCMS_dictionary.md", "yadaAI_dictionary.md", "index.md"]:
		target = OUTPUT_DIR / filename
		if target.exists():
			target.unlink()

	top_lines = [
		"# Yada Code Dictionary",
		"",
		"| Module | Entry Point | Description |",
		"|---|---|---|",
	]
	for module_info in MODULES.values():
		top_lines.append(f"| {module_info['title']} | [{module_info['entry']}]({module_info['entry']}) | {module_info['summary']} |")
	top_lines.append("")
	(OUTPUT_DIR / "yada_dictionary.md").write_text("\n".join(top_lines), encoding="utf-8")

	index_lines = [
		"# Yada AI Knowledge Index",
		"",
		"| File | Read this when you need... |",
		"|---|---|",
		"| `aiknowledge/yada_dictionary.md` | the entry point for the generated Yada code dictionary |",
		"| `aiknowledge/yadaWeb_dictionary.md` | the public YadaWeb utility, persistence, and DataTables API surface |",
		"| `aiknowledge/yadaWebSecurity_dictionary.md` | the YadaWebSecurity controllers, helpers, and DAO surface |",
		"| `aiknowledge/yadaWebCMS_dictionary.md` | the YadaWebCMS helpers and persistence API surface |",
		"| `aiknowledge/yadaAI_dictionary.md` | the YadaAI helper and Claude request-builder API surface |",
		"",
	]
	(OUTPUT_DIR / "index.md").write_text("\n".join(index_lines), encoding="utf-8")

	for module_key, types in types_by_module.items():
		module_info = MODULES[module_key]
		module_lines = [f"# {module_info['title']} Code Dictionary", ""]
		package_map: dict[str, list[TypeInfo]] = defaultdict(list)
		for type_info in types:
			package_map[type_info.package].append(type_info)
		for package in sorted(package_map):
			module_lines.append(f"## `{package}`")
			module_lines.append("")
			module_lines.append("| Class | Description |")
			module_lines.append("|---|---|")
			for type_info in sorted(package_map[package], key=lambda item: item.name.lower()):
				module_lines.append(f"| [{type_info.name}]({module_info['folder']}/{type_info.name}.md) | {class_description(type_info)} |")
			module_lines.append("")
		(OUTPUT_DIR / module_info["entry"]).write_text("\n".join(module_lines), encoding="utf-8")

		module_folder = OUTPUT_DIR / module_info["folder"]
		for type_info in types:
			class_lines = [f"# `{type_info.full_name}`", "", "| Method | Description |", "|---|---|"]
			for method in type_info.methods:
				class_lines.append(f"| `{method.name}` | {method_description(type_info, method)} |")
			class_lines.append("")
			(module_folder / f"{type_info.name}.md").write_text("\n".join(class_lines), encoding="utf-8")


def collect_types() -> dict[str, list[TypeInfo]]:
	types_by_module: dict[str, list[TypeInfo]] = defaultdict(list)
	for module_key, module_info in MODULES.items():
		for path in sorted(module_info["source"].rglob("*.java")):
			if path.name == "package-info.java":
				continue
			type_info = parse_java_file(module_key, path)
			if type_info is None or not keep_type(type_info):
				continue
			if not type_info.methods:
				type_info.methods = fallback_public_methods(path.read_text(encoding="utf-8"))
			methods = dedupe_methods([method for method in type_info.methods if keep_method(type_info, method)])
			if not methods:
				continue
			type_info.methods = methods
			types_by_module[module_key].append(type_info)
	for module_key in list(types_by_module.keys()):
		types_by_module[module_key] = sorted(types_by_module[module_key], key=lambda item: (item.package, item.name.lower()))
	return types_by_module


def main() -> None:
	write_markdown(collect_types())


if __name__ == "__main__":
	main()

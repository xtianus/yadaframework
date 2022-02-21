package net.yadaframework.security.persistence.entity;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unbescape.html.HtmlEscape;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaLocalEnum;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.persistence.entity.YadaAttachedFile;
import net.yadaframework.persistence.entity.YadaPersistentEnum;
import net.yadaframework.web.YadaJsonDateTimeShortSerializer;

/**
 * A message sent to some user by another user or by the system.
 * Identical consecutive messages can be "stacked" on a single row by incrementing the "stackSize" and adding a new "created" date
 *
 * @param <E> a localized enum for the message type
 */
@Entity
// Use a joined table for inheritance so that the subclasses can independently add any attributes without interference
@Inheritance(strategy = InheritanceType.JOINED)
// Should actually be YadaUserMessage<YLE extends YadaLocalEnum<E extends Enum<E>>
// so that every subclass of YadaPersistentEnum handles its own Enum<E> type
// and I get compiler error on a mismatched setType/getType
// but apparently there is no way of doing this in Java 8.
// Using YadaUserMessage<E extends Enum<E>> fixes getType/setType but would cause a runtime error when E is a plain enum
// because the type attribute should be a YadaPersistentEnum as there is a foreign key in the database.
// So I use YadaUserMessage<YLE extends YadaLocalEnum<?>> but I may need to check the enum consistency at runtime
// because the compiler can't do that. There will also be a runtime error on a mismatched setType/getType.
public class YadaUserMessage<YLE extends YadaLocalEnum<?>> implements Serializable {
	private static final long serialVersionUID = 7008892353441772768L;
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	@Version
	protected long version; // For optimistic locking

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;

	protected int priority; // Priority or severity, 0 is lowest

	protected boolean readByRecipient = false; // Read by recipient

	protected boolean emailed = false; // Emailed to recipient

	@ElementCollection
	@Temporal(TemporalType.TIMESTAMP)
	//@JsonView(YadaJsonView.WithEagerAttributes.class)
	//@JsonView(YadaJsonView.WithLazyAttributes.class)
	protected List<Date> created; // Creation date of the message, a new date is added for each stacked message

	@Column(insertable = false, updatable = false, columnDefinition="TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date modified = new Date();

	protected int stackSize = 0; // Counter for identical messages (stacked)

	@OneToOne(fetch = FetchType.EAGER, optional=true)
	protected YadaPersistentEnum<?> type;

	@Column(length=80)
	protected String title;

	@Column(length=12000) // Should it be Lob?
	protected String message;

	@ManyToOne(optional = true)
	protected YadaUserProfile sender;

	@ManyToOne(optional = true)
	protected YadaUserProfile recipient;

	//@JsonView(YadaJsonView.WithLazyAttributes.class)
	@OneToMany(cascade=CascadeType.ALL, orphanRemoval=true) // It was REMOVE - why?
	protected List<YadaAttachedFile> attachment = new ArrayList<>();

	@Column(length=1024)
	protected String data; // Can store a url here, or the identity of a non-user sender

	protected int contentHash; // To check message equality for stackability

	protected Integer status; // Application-defined status for this message

	////////////////////////////////////////////

	@Transient
	protected boolean stackable; // true if same-content messages should be counted not added

	private Type getCurrentEnumType() {
		return ((java.lang.reflect.ParameterizedType)this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}

	/**
	 * Used in Datatables to define the row class.
	 * @See {@link https://datatables.net/manual/server-side}
	 */
	@JsonProperty("DT_RowClass")
	public String getDT_RowClass() {
		// We set the class according to the read state of the message
		return this.readByRecipient?"yadaUserMessage-read":"yadaUserMessage-unread";
	}

	/**
	 * Computes the content hash before persisting.
	 * The hash does not consider attachments, sender, receiver or status flags.
	 * Override this method if your subclass add more fields that should be checked when computing stackability of messages
	 */
	@PrePersist
	public void init() {
		computeHash();
		setInitialDate();
	}

	/**
	 * Escape all markup-significant characters
	 * @param message
	 * @return
	 * @return the current instance
	 */
	public YadaUserMessage<YLE> setMessageEscaped(String message) {
		this.message = HtmlEscape.escapeHtml5Xml(message);
		return this;
	}

	public void computeHash() {
		// The type is null when the YadaPersistentEnum has not been initialized in the database
		if (type==null) {
			// get the type of the enum: if it is a YadaPersistentEnum throw an exception with info
			Class<?> genericClass = YadaUtil.INSTANCE.findGenericClass(this);
//			boolean isYadaLocalEnum = YadaLocalEnum.class.isAssignableFrom(genericClass);
//			if (isYadaLocalEnum) {
//				Type missingEnum = null;
//				try {
//					missingEnum = getCurrentEnumType();
//				} catch (Exception e) {
//					// Ignored
//				}
//				String enumName = missingEnum!=null?missingEnum.toString():"all YadaLocalEnum classes";
//			}
			throw new YadaInternalException("YadaUserMessage 'type' is null - did you remember to add " + genericClass + " to yadaPersistentEnumDao.initDatabase() during application setup?");
		}
		this.contentHash = Objects.hash(type==null?null:type.getEnum(), title, message, data);
	}

	public void setInitialDate() {
		if (created==null) {
			created = new ArrayList<Date>();
			created.add(new Date());
		}
	}

	/**
	 * Returns the most recent date of the message stack - which is the initial date if the message is not stackable
	 * @return
	 */
	public Date getLastDate() {
		if (created!=null && created.size()>0) {
			return created.get(created.size()-1);
		}
		return null;
	}

	public void incrementStack() {
		this.stackSize++;
		this.created.add(new Date());
	}

	public void setType(YLE localEnum) {
		this.type = localEnum.toYadaPersistentEnum();
	}

	/***********************************************************************/
	/* DataTables                                                          */

	@Transient
	@JsonProperty("DT_RowId")
	@Deprecated // can be removed now that it is added automatically by YadaDataTableDao
	public String getDT_RowId() {
		return this.getClass().getSimpleName()+"#"+this.id; // YadaUserMessage#142
	}

	@Transient
	@JsonProperty
	public String getSenderName() {
		if (sender!=null) {
			return sender.getUserCredentials().getUsername();
		}
		return null;
	}

	@Transient
	@JsonProperty
	public String getReceiverName() {
		return recipient!=null?recipient.getUserCredentials().getUsername():"-";
	}

	/** Adds a new attachment to this message
	 *
	 */
	public void addAttachment(YadaAttachedFile newAttachment) {
		attachment.add(newAttachment);
	}

	/***********************************************************************/
	/* Plain getter / setter                                               */
	@JsonSerialize(using=YadaJsonDateTimeShortSerializer.class)
	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public boolean isEmailed() {
		return emailed;
	}

	public void setEmailed(boolean emailed) {
		this.emailed = emailed;
	}

	public int getStackSize() {
		return stackSize;
	}

	public void setStackSize(int count) {
		this.stackSize = count;
	}

	public YadaPersistentEnum<?> getType() {
		return type;
	}

	public void setType(YadaPersistentEnum<?> type) {
		this.type = type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public YadaUserProfile getSender() {
		return sender;
	}

	public void setSender(YadaUserProfile senderUser) {
		this.sender = senderUser;
	}

	public YadaUserProfile getRecipient() {
		return recipient;
	}

	public void setRecipient(YadaUserProfile recipient) {
		this.recipient = recipient;
	}

	public List<YadaAttachedFile> getAttachment() {
		return attachment;
	}

	public void setAttachment(List<YadaAttachedFile> attachment) {
		this.attachment = attachment;
	}

	public int getContentHash() {
		return contentHash;
	}

	public void setContentHash(int hash) {
		this.contentHash = hash;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public boolean isStackable() {
		return stackable;
	}

	public void setStackable(boolean stackable) {
		this.stackable = stackable;
	}

	public List<Date> getCreated() {
		return created;
	}

	public void setCreated(List<Date> created) {
		this.created = created;
	}

	public boolean isReadByRecipient() {
		return readByRecipient;
	}

	public void setReadByRecipient(boolean readByRecipient) {
		this.readByRecipient = readByRecipient;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

}

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.yadaframework.core.YadaLocalEnum;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.persistence.entity.YadaAttachedFile;
import net.yadaframework.persistence.entity.YadaPersistentEnum;
import net.yadaframework.web.YadaJsonDateTimeShortSerializer;
import net.yadaframework.web.YadaJsonView;

/**
 * A message sent to some user by another user or by the system.
 * Identical consecutive messages can be "stacked" on a single row by incrementing the "stackSize" and adding a new "created" date
 * 
 * @param <E> a localized enum for the message type
 */
@Entity
// Use a joined table for inheritance so that the subclasses can independently add any attributes without interference
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaUserMessage<E extends Enum<E>> implements Serializable {
	private static final long serialVersionUID = 7008892353441772768L;
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	@Version
	protected long version; // For optimistic locking
	
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;
	
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected int priority; // Priority or severity, 0 is lowest
	
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected boolean readByRecipient = false; // Read by recipient
	
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected boolean emailed = false; // Emailed to recipient
	
	@ElementCollection
	@Temporal(TemporalType.TIMESTAMP)
	//@JsonView(YadaJsonView.WithEagerAttributes.class)
	//@JsonView(YadaJsonView.WithLazyAttributes.class)
	protected List<Date> created; // Creation date of the message, a new date is added for each stacked message

	@JsonView(YadaJsonView.WithEagerAttributes.class)
	@Column(columnDefinition="TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date modified = new Date();
	
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected int stackSize = 0; // Counter for identical messages (stacked)

	@OneToOne(fetch = FetchType.EAGER, optional=true)
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected YadaPersistentEnum<E> type;

	@Column(length=80)
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected String title;
	
	@Column(length=8192)
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected String message;
	
	@ManyToOne(optional = true)
	@JsonView(YadaJsonView.WithLazyAttributes.class)
	protected YadaUserProfile sender;
	
	@ManyToOne(optional = true)
	@JsonView(YadaJsonView.WithLazyAttributes.class)
	protected YadaUserProfile recipient;
	
	//@JsonView(YadaJsonView.WithLazyAttributes.class)
	@OneToMany(cascade=CascadeType.ALL, orphanRemoval=true) // It was REMOVE - why?
	protected List<YadaAttachedFile> attachment = new ArrayList<>();
	
	@Column(length=1024)
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected String data; // Can store a url here, or the identity of a non-user sender
	
	protected int contentHash; // To check message equality for stackability
	
	@Transient
	protected boolean stackable; // true if same-content messages should be counted not added 

	/**
	 * Used in Datatables to define the row class.
	 * @See {@link https://datatables.net/manual/server-side}
	 */
	@JsonView(YadaJsonView.WithEagerAttributes.class)
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
	
	public void computeHash() {
		if (type==null) {
			Type missingEnum = null;
			try {
				missingEnum = ((java.lang.reflect.ParameterizedType)this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
			} catch (Exception e) {
				// Ignored
			}
			String enumName = missingEnum!=null?missingEnum.toString():"all YadaLocalEnum classes";
			throw new YadaInternalException("YadaUserMessage type is null - did you remember to add " + enumName + " to yadaPersistentEnumDao.initDatabase() during application setup?");
		}
		this.contentHash = Objects.hash(type.getEnum(), title, message, data);
	}

	public void setInitialDate() {
		if (created==null) {
			created = new ArrayList<Date>();
			created.add(new Date());
		}
	}
	
	public void incrementStack() {
		this.stackSize++;
		this.created.add(new Date());
	}
	
	public void setType(YadaLocalEnum<E> localEnum) {
		this.type = localEnum.toYadaPersistentEnum();
	}
	
	/***********************************************************************/
	/* DataTables                                                          */
	
	@Transient
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	@JsonProperty("DT_RowId")
	@Deprecated // can be removed now that it is added automatically by YadaDataTableDao
	public String getDT_RowId() {
		return this.getClass().getSimpleName()+"#"+this.id; // YadaUserMessage#142
	}
	
	@Transient
	@JsonProperty
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	public String getSenderName() {
		if (sender!=null) {
			return sender.getUserCredentials().getUsername();
		}
		return null;
	}
	
	@Transient
	@JsonProperty
	@JsonView(YadaJsonView.WithEagerAttributes.class)
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

	public YadaPersistentEnum<E> getType() {
		return type;
	}

	public void setType(YadaPersistentEnum<E> type) {
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

	
}

package net.yadaframework.security.persistence.entity;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaCommentMessage extends YadaUserMessage<YadaUserMessageType> {
	private static final long serialVersionUID = 1L;

	private long totLikes;
	private int totReplies; // Counter for comment first-level replies

	@ManyToMany
	@JoinTable(name="YadaCommentMessageLiked", joinColumns = @JoinColumn(name = "YadaCommentMessage_id"), inverseJoinColumns = @JoinColumn(name = "YadaUserProfile_id"))
	private Set<YadaUserProfile> likers;

	// This message is a reply to another one.
	@ManyToOne
	protected YadaCommentMessage repliesTo;

	public YadaCommentMessage() {
		super.setType(YadaUserMessageType.COMMENT);
	}

	public Set<YadaUserProfile> getLikers() {
		return likers;
	}

	public void setLikers(Set<YadaUserProfile> likers) {
		this.likers = likers;
	}

	public long getTotLikes() {
		return totLikes;
	}

	public void setTotLikes(long totLikes) {
		this.totLikes = totLikes;
	}

	public int getTotReplies() {
		return totReplies;
	}

	public void setTotReplies(int totReplies) {
		this.totReplies = totReplies;
	}

}

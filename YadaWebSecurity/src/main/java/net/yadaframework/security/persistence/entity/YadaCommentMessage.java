package net.yadaframework.security.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import java.util.Set;

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

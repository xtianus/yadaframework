package net.yadaframework.cms.persistence.entity;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.CloneableDeep;
import net.yadaframework.persistence.entity.YadaAttachedFile;

/**
 * Stores all the elements that can appear on a gallery slide: image, video, text...
 * A "gallery" is not necessarily a carousel: the presentation can be anything.
 */
// We keep it simple and use a discriminator for inheritance.
@Entity
public class YadaGallerySlide implements CloneableDeep {
	@Version
	private long version; // For optimistic locking

	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	private Long id;

	private boolean slideEnabled = true;

	// Position in the sequence to which the slide belongs.
	// Automatically set to the id on persist. Can be swapped with another pos for sorting.
	private Long pos = null;

	private boolean flag1 = false; // This could be used e.g. for choosing an alternative layout of the single slide
	private boolean flag2 = false; // This could be used e.g. for choosing an alternative layout of the single slide

	// Generic attributes for each slide, e.g. position of the crop frame, text color...
	private String data1;
	private String data2;
	private String data3;
	private String data4;
	private String data5;
	private String data6;

	@OneToOne(cascade= {CascadeType.PERSIST, CascadeType.REMOVE}) // Remember to delete the file from disk on REMOVE
	private YadaAttachedFile video;

	@OneToOne(cascade= {CascadeType.PERSIST, CascadeType.REMOVE}) // Remember to delete the file from disk on REMOVE
	private YadaAttachedFile image;

	// Single language texts

	@Column(length=2048)
	private String text1;

	@Column(length=2048)
	private String text2;

	@Column(length=2048)
	private String text3;

	@Column(length=2048)
	private String text4;

	// Multi-language texts

	@ElementCollection
	@Column(length=2048)
	@MapKeyColumn(name="locale", length=32)
	private Map<Locale, String> text5local = new HashMap<>();

	@ElementCollection
	@Column(length=2048)
	@MapKeyColumn(name="locale", length=32)
	private Map<Locale, String> text6local = new HashMap<>();

	@ElementCollection
	@Column(length=2048)
	@MapKeyColumn(name="locale", length=32)
	private Map<Locale, String> text7local = new HashMap<>();

	@ElementCollection
	@Column(length=2048)
	@MapKeyColumn(name="locale", length=32)
	private Map<Locale, String> text8local = new HashMap<>();

	/////////////////////////////////////////////////////////////
	// Transients for POST

	@Transient
	private MultipartFile multipartImage;

	@Transient
	private MultipartFile multipartVideo;

	/////////////////////////////////////////////////////////////

	@PostPersist
	private void postPersist() {
		if (this.pos==null) {
			// The check is needed to preserve the position on copy
			this.pos = this.id;
		}
	}

	@Override
	public Field[] getExcludedFields() {
		return null;
	}

	public String getText5LocalValue() {
		 return YadaUtil.getLocalValue(text5local);
	}

	public String getText6LocalValue() {
		return YadaUtil.getLocalValue(text6local);
	}

	public String getText7LocalValue() {
		return YadaUtil.getLocalValue(text7local);
	}

	public String getText8LocalValue() {
		return YadaUtil.getLocalValue(text8local);
	}

	/////////////////////////////////////////////////////////////
	// Getter/Setter

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public YadaAttachedFile getVideo() {
		return video;
	}

	public void setVideo(YadaAttachedFile video) {
		this.video = video;
	}

	public YadaAttachedFile getImage() {
		return image;
	}

	public void setImage(YadaAttachedFile image) {
		this.image = image;
	}

	public String getText1() {
		return text1;
	}

	public void setText1(String text1) {
		this.text1 = text1;
	}

	public String getText2() {
		return text2;
	}

	public void setText2(String text2) {
		this.text2 = text2;
	}

	public String getText3() {
		return text3;
	}

	public void setText3(String text3) {
		this.text3 = text3;
	}

	public Map<Locale, String> getText5local() {
		return text5local;
	}

	public void setText5local(Map<Locale, String> text5local) {
		this.text5local = text5local;
	}

	public Map<Locale, String> getText6local() {
		return text6local;
	}

	public void setText6local(Map<Locale, String> text6local) {
		this.text6local = text6local;
	}

	public Long getPos() {
		return pos;
	}

	public void setPos(Long pos) {
		this.pos = pos;
	}

	public boolean isSlideEnabled() {
		return slideEnabled;
	}

	public void setSlideEnabled(boolean enabled) {
		this.slideEnabled = enabled;
	}

	public boolean isFlag1() {
		return flag1;
	}

	public void setFlag1(boolean flag1) {
		this.flag1 = flag1;
	}

	public boolean isFlag2() {
		return flag2;
	}

	public void setFlag2(boolean flag2) {
		this.flag2 = flag2;
	}

	public String getText4() {
		return text4;
	}

	public void setText4(String text4) {
		this.text4 = text4;
	}

	public Map<Locale, String> getText7local() {
		return text7local;
	}

	public void setText7local(Map<Locale, String> text7local) {
		this.text7local = text7local;
	}

	public Map<Locale, String> getText8local() {
		return text8local;
	}

	public void setText8local(Map<Locale, String> text8local) {
		this.text8local = text8local;
	}

	public String getData1() {
		return data1;
	}

	public void setData1(String data1) {
		this.data1 = data1;
	}

	public String getData2() {
		return data2;
	}

	public void setData2(String data2) {
		this.data2 = data2;
	}

	public String getData3() {
		return data3;
	}

	public void setData3(String data3) {
		this.data3 = data3;
	}

	public String getData4() {
		return data4;
	}

	public void setData4(String data4) {
		this.data4 = data4;
	}

	public String getData5() {
		return data5;
	}

	public void setData5(String data5) {
		this.data5 = data5;
	}

	public String getData6() {
		return data6;
	}

	public void setData6(String data6) {
		this.data6 = data6;
	}

	public MultipartFile getMultipartImage() {
		return multipartImage;
	}

	public void setMultipartImage(MultipartFile multipartImage) {
		this.multipartImage = multipartImage;
	}

	public MultipartFile getMultipartVideo() {
		return multipartVideo;
	}

	public void setMultipartVideo(MultipartFile multipartVideo) {
		this.multipartVideo = multipartVideo;
	}

	@Override
	public int hashCode() {
		if (id!=null) {
			return Objects.hash(id);
		} else {
			return hashCodeInternal();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		YadaGallerySlide other = (YadaGallerySlide) obj;
		if (id!=null) {
			return Objects.equals(id, other.id);
		} else {
			return equalsInternal(other);
		}
	}

	public int hashCodeInternal() {
		return Objects.hash(data1, data2, data3, data4, data5, data6, flag1, flag2, id, image, 
				pos, slideEnabled, text1, text2, text3, text4, text5local, text6local, text7local,
				text8local, video);
	}

	public boolean equalsInternal(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		YadaGallerySlide other = (YadaGallerySlide) obj;
		return Objects.equals(data1, other.data1) && Objects.equals(data2, other.data2)
				&& Objects.equals(data3, other.data3) && Objects.equals(data4, other.data4)
				&& Objects.equals(data5, other.data5) && Objects.equals(data6, other.data6) && flag1 == other.flag1
				&& flag2 == other.flag2 && Objects.equals(id, other.id) && Objects.equals(image, other.image)
				&& Objects.equals(pos, other.pos)
				&& slideEnabled == other.slideEnabled && Objects.equals(text1, other.text1)
				&& Objects.equals(text2, other.text2) && Objects.equals(text3, other.text3)
				&& Objects.equals(text4, other.text4) && Objects.equals(text5local, other.text5local)
				&& Objects.equals(text6local, other.text6local) && Objects.equals(text7local, other.text7local)
				&& Objects.equals(text8local, other.text8local) && Objects.equals(video, other.video);
	}

}

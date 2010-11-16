package ee.ioc.phon.tsab.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity(name="transcriptionfragment")
@Table(name="transcriptionfragment")
public class TranscriptionFragment {

  private Long id;
  private Transcription transcription;
  private Long time;
  private String author;
  private String text;

  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @ManyToOne(optional=false)
  public Transcription getTranscription() {
    return transcription;
  }

  public void setTranscription(Transcription transcription) {
    this.transcription = transcription;
  }

  @Column
  public Long getTime() {
    return time;
  }

  public void setTime(Long time) {
    this.time = time;
  }

  @Column
  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  @Column(length=10000)
  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

}

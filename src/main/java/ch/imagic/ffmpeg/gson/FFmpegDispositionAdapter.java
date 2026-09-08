package ch.imagic.ffmpeg.gson;

import ch.imagic.ffmpeg.probe.FFmpegDisposition;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Optional;

/** Converts FFprobe's numeric or boolean disposition flags to Java booleans. */
public class FFmpegDispositionAdapter extends TypeAdapter<FFmpegDisposition> {

    protected Optional<Boolean> readBoolean(JsonReader reader) throws IOException {
        JsonToken next = reader.peek();
        switch (next) {
            case BOOLEAN:
                return Optional.of(reader.nextBoolean());
            case NUMBER:
                return Optional.of(reader.nextInt() != 0);
            default:
                reader.skipValue();
                return Optional.empty();
        }
    }

    private void setProperty(FFmpegDisposition disposition, String name, boolean value) {
        switch (name) {
            case "default":
                disposition.setDefaultDisposition(value);
                break;
            case "dub":
                disposition.setDub(value);
                break;
            case "original":
                disposition.setOriginal(value);
                break;
            case "comment":
                disposition.setComment(value);
                break;
            case "lyrics":
                disposition.setLyrics(value);
                break;
            case "karaoke":
                disposition.setKaraoke(value);
                break;
            case "forced":
                disposition.setForced(value);
                break;
            case "hearing_impaired":
                disposition.setHearingImpaired(value);
                break;
            case "visual_impaired":
                disposition.setVisualImpaired(value);
                break;
            case "clean_effects":
                disposition.setCleanEffects(value);
                break;
            case "attached_pic":
                disposition.setAttachedPic(value);
                break;
            case "captions":
                disposition.setCaptions(value);
                break;
            case "descriptions":
                disposition.setDescriptions(value);
                break;
            case "metadata":
                disposition.setMetadata(value);
                break;
            default:
                break;
        }
    }

    @Override
    public FFmpegDisposition read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }

        FFmpegDisposition disposition = new FFmpegDisposition();
        reader.beginObject();
        while (reader.peek() != JsonToken.END_OBJECT) {
            String name = reader.nextName();
            Optional<Boolean> value = readBoolean(reader);
            value.ifPresent(flag -> setProperty(disposition, name, flag));
        }
        reader.endObject();
        return disposition;
    }

    @Override
    public void write(JsonWriter writer, FFmpegDisposition disposition) throws IOException {
        if (disposition == null) {
            writer.nullValue();
            return;
        }

        writer.beginObject();
        writer.name("default").value(disposition.isDefaultDisposition());
        writer.name("dub").value(disposition.isDub());
        writer.name("original").value(disposition.isOriginal());
        writer.name("comment").value(disposition.isComment());
        writer.name("lyrics").value(disposition.isLyrics());
        writer.name("karaoke").value(disposition.isKaraoke());
        writer.name("forced").value(disposition.isForced());
        writer.name("hearing_impaired").value(disposition.isHearingImpaired());
        writer.name("visual_impaired").value(disposition.isVisualImpaired());
        writer.name("clean_effects").value(disposition.isCleanEffects());
        writer.name("attached_pic").value(disposition.isAttachedPic());
        writer.name("captions").value(disposition.isCaptions());
        writer.name("descriptions").value(disposition.isDescriptions());
        writer.name("metadata").value(disposition.isMetadata());
        writer.endObject();
    }
}

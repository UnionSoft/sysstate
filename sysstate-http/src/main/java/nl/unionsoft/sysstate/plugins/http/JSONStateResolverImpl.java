package nl.unionsoft.sysstate.plugins.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import nl.unionsoft.sysstate.common.dto.InstanceDto;
import nl.unionsoft.sysstate.common.dto.StateDto;
import nl.unionsoft.sysstate.common.enums.StateType;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//@Service("jsonStateResolver")
public class JSONStateResolverImpl extends HttpStateResolverImpl {

    private static final Logger LOG = LoggerFactory.getLogger(JSONStateResolverImpl.class);

    @Override
    public void handleEntity(final HttpEntity httpEntity, final  Map<String, String> configuration, final StateDto state, InstanceDto instance) throws IOException {
        InputStream contentStream = null;
        try {
            if (httpEntity != null) {
                contentStream = httpEntity.getContent();
                //                Gson gson = new Gson();
                //                JsonReader json = new JsonReader(new InputStreamReader(contentStream));
                // while (json.hasNext()) {
                // // json.next
                //                }
                //                JsonElement jsonElement = new JsonElement() {
                //                };
                // gson.fromJson(json, classOfT)

                // handleGSON(document, configuration, state);
            }
        } catch(final IllegalStateException e) {
            LOG.error("Caught IllegalStateException", e);
            state.setState(StateType.ERROR);
            state.setDescription(e.getMessage());

        } catch(final IOException e) {
            LOG.error("Caught IOException", e);
            state.setState(StateType.ERROR);
            state.setDescription(e.getMessage());
        } finally {
            IOUtils.closeQuietly(contentStream);
        }
    }

}

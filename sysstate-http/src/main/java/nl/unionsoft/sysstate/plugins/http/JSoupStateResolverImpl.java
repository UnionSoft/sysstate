package nl.unionsoft.sysstate.plugins.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import nl.unionsoft.sysstate.common.dto.InstanceDto;
import nl.unionsoft.sysstate.common.dto.StateDto;
import nl.unionsoft.sysstate.common.enums.StateType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("jsoupStateResolver")
public class JSoupStateResolverImpl extends HttpStateResolverImpl {

    private static final Logger LOG = LoggerFactory.getLogger(JSoupStateResolverImpl.class);

    @Override
    public void handleEntity(final HttpEntity httpEntity, final Map<String, String> configuration, final StateDto state,  InstanceDto instance) throws IOException {
        InputStream contentStream = null;
        try {
            if (httpEntity != null) {
                contentStream = httpEntity.getContent();
                final Document document = Jsoup.parse(contentStream, "UTF-8", configuration.get(URL), Parser.xmlParser());
                handleJsoup(document, configuration, state);
            }
        } catch (final IllegalStateException e) {
            LOG.error("Caught IllegalStateException", e);
            state.setState(StateType.ERROR);
            state.setDescription(e.getMessage());

        } catch (final IOException e) {
            LOG.error("Caught IOException", e);
            state.setState(StateType.ERROR);
            state.setDescription(e.getMessage());
        } finally {
            IOUtils.closeQuietly(contentStream);
        }
    }

    public void handleJsoup(final Document document, final Map<String, String> configuration, final StateDto state) {

        final String select = configuration.get("select");
        handleSelect(document, select, state);
    }

    public void handleSelect(final Document document, final String select, final StateDto state) {
        LOG.info("Select is: {}", select);
        final Elements elements = document.select(select);
        if (elements == null || elements.size() == 0) {
            state.setState(StateType.UNSTABLE);
            state.appendMessage("No elements found for select '" + select + "'.");
        } else {
            String description = null;
            for (final Element element : elements) {
                description = element.text();
                if (StringUtils.isNotBlank(description)) {
                    state.setDescription(description);
                    break;
                }
            }
            if (StringUtils.isBlank(description)) {
                state.setState(StateType.UNSTABLE);
                state.appendMessage("Result for given select returned a empty value.");
            }

        }
    }
}

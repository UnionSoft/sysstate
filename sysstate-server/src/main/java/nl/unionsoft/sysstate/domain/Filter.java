package nl.unionsoft.sysstate.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import nl.unionsoft.sysstate.common.enums.StateType;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "SSE_FILTER")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Filter {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME", nullable = true, length = 255)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "SSE_FILTER_PROJECT")
    @Column(name = "PROJECTS", nullable = true)
    @Fetch(FetchMode.SELECT)
    private List<Long> projects;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "SSE_FILTER_ENVIRONMENT")
    @Column(name = "ENVIRONMENTS", nullable = true)
    @Fetch(FetchMode.SELECT)
    private List<Long> environments;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "SSE_FILTER_STATE")
    @Column(name = "STATES", nullable = true)
    @Fetch(FetchMode.SELECT)
    private List<StateType> states;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "SSE_FILTER_RESOLVER")
    @Column(name = "STATE_RESOLVERS", nullable = true)
    @Fetch(FetchMode.SELECT)
    private List<String> stateResolvers;

    @Column(name = "TAGS", nullable = true, length = 255)
    private String tags;

    @Column(name = "SEARCH", nullable = true, length = 255)
    private String search;

    @OneToMany(mappedBy = "filter", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<View> views;

    public Filter () {
        projects = new ArrayList<Long>();
        environments = new ArrayList<Long>();
        states = new ArrayList<StateType>();
        stateResolvers = new ArrayList<String>();
        views = new ArrayList<View>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Long> getProjects() {
        return projects;
    }

    public void setProjects(List<Long> projects) {
        this.projects = projects;
    }

    public List<Long> getEnvironments() {
        return environments;
    }

    public void setEnvironments(List<Long> environments) {
        this.environments = environments;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public List<StateType> getStates() {
        return states;
    }

    public void setStates(List<StateType> states) {
        this.states = states;
    }

    public List<String> getStateResolvers() {
        return stateResolvers;
    }

    public void setStateResolvers(List<String> stateResolvers) {
        this.stateResolvers = stateResolvers;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<View> getViews() {
        return views;
    }

    public void setViews(List<View> views) {
        this.views = views;
    }

}

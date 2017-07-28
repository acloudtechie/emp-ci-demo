package net.micropact.aea.rf.dao;

import java.util.List;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.dynamic.RfScript;

import net.entellitrak.aea.rf.dao.IRfScript;
import net.entellitrak.aea.rf.dao.IRfScriptParameter;
import net.entellitrak.aea.rf.dao.IScriptObject;
import net.micropact.aea.rf.service.RfDaoServicePrivate;

/**
 * Simple implementation of {@link IRfScript}.
 *
 * @author zachary.miller
 */
public class RfScriptImpl implements IRfScript {
    private final RfDaoServicePrivate rfDaoService;

    private final long trackingId;
    private final String name;
    private final String scriptObjectName;
    private IScriptObject scriptObject;
    private final String code;
    private final String description;
    private List<IRfScriptParameter> scriptParameters;

    /**
     * A simple constructor.
     *
     * @param etk entellitrak execution context
     * @param daoService service for lazily loading related objects
     * @param rfScriptId tracking id of the RF Script object
     */
    public RfScriptImpl(final ExecutionContext etk, final RfDaoServicePrivate daoService, final long rfScriptId){
        rfDaoService = daoService;
        final RfScript rfScript = etk.getDynamicObjectService().get(RfScript.class, rfScriptId);

        trackingId = rfScriptId;
        name = rfScript.getName();
        scriptObjectName = rfScript.getScriptObject();
        code = rfScript.getCode();
        description = rfScript.getDescription();
    }

    @Override
    public long getId(){
        return trackingId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public IScriptObject getScriptObject() {
        if(scriptObject == null){
            scriptObject = RfDaoServicePrivate.loadScriptObject(scriptObjectName);
        }
        return scriptObject;
    }

    @Override
    public List<IRfScriptParameter> getScriptParameters() throws IncorrectResultSizeDataAccessException {
        if(scriptParameters == null){
            scriptParameters = rfDaoService.loadScriptParametersByRfScriptId(trackingId);
        }
        return scriptParameters;
    }
}

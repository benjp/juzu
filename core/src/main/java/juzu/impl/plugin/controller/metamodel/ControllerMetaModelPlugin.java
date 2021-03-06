/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package juzu.impl.plugin.controller.metamodel;

import juzu.Action;
import juzu.Application;
import juzu.Consumes;
import juzu.Resource;
import juzu.View;
import juzu.impl.common.Name;
import juzu.impl.plugin.application.metamodel.ApplicationMetaModel;
import juzu.impl.plugin.application.metamodel.ApplicationMetaModelPlugin;
import juzu.impl.metamodel.AnnotationKey;
import juzu.impl.metamodel.AnnotationState;
import juzu.impl.compiler.ProcessingException;
import juzu.impl.compiler.ElementHandle;
import juzu.impl.compiler.ProcessingContext;
import juzu.impl.request.ContextualParameter;
import juzu.impl.request.Method;
import juzu.impl.request.Parameter;
import juzu.impl.request.PhaseParameter;
import juzu.impl.plugin.controller.descriptor.ControllerDescriptor;
import juzu.impl.metamodel.MetaModelEvent;
import juzu.impl.metamodel.MetaModelObject;
import juzu.impl.request.Request;
import juzu.impl.common.Cardinality;
import juzu.impl.common.JSON;
import juzu.impl.common.Tools;
import juzu.request.Phase;

import javax.annotation.Generated;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class ControllerMetaModelPlugin extends ApplicationMetaModelPlugin {

  /** . */
  private static final String METHOD_DESCRIPTOR = Method.class.getSimpleName();

  /** . */
  private static final String CONTROLLER_DESCRIPTOR = ControllerDescriptor.class.getSimpleName();

  /** . */
  private static final String PARAMETER = Parameter.class.getSimpleName();

  /** . */
  private static final String INVOCATION_PARAMETER = PhaseParameter.class.getSimpleName();

  /** . */
  private static final String CONTEXTUAL_PARAMETER = ContextualParameter.class.getSimpleName();

  /** . */
  private static final String PHASE = Phase.class.getSimpleName();

  /** . */
  private static final String TOOLS = Tools.class.getSimpleName();

  /** . */
  public static final String CARDINALITY = Cardinality.class.getSimpleName();

  /** . */
  private static final Set<Name> NAMES = Tools.set(
      Name.create(Action.class),
      Name.create(Consumes.class),
      Name.create(View.class),
      Name.create(Resource.class));

  /** . */
  private HashSet<ControllerMetaModel> written = new HashSet<ControllerMetaModel>();

  public ControllerMetaModelPlugin() {
    super("controller");
  }

  public Set<Class<? extends java.lang.annotation.Annotation>> init(ProcessingContext env) {
    return Tools.<Class<? extends java.lang.annotation.Annotation>>set(View.class, Action.class, Consumes.class, Resource.class);
  }

  @Override
  public void init(ApplicationMetaModel application) {
    ControllersMetaModel controllers = new ControllersMetaModel();
    PackageElement pkg = application.model.processingContext.get(application.getHandle());
    AnnotationMirror annotation = Tools.getAnnotation(pkg, Application.class.getName());
    AnnotationState values = AnnotationState.create(annotation);
    Boolean escapeXML = (Boolean)values.get("escapeXML");
    ElementHandle.Class defaultControllerElt = (ElementHandle.Class)values.get("defaultController");
    controllers.escapeXML = escapeXML;
    controllers.defaultController = defaultControllerElt != null ? defaultControllerElt.getFQN() : null;
    application.addChild(ControllersMetaModel.KEY, controllers);
  }

  @Override
  public void processAnnotationAdded(ApplicationMetaModel application, AnnotationKey key, AnnotationState added) {
    if (NAMES.contains(key.getType())) {
      ControllersMetaModel ac = application.getChild(ControllersMetaModel.KEY);
      ElementHandle.Method m = (ElementHandle.Method)key.getElement();
      ElementHandle.Class handle = ElementHandle.Class.create(m.getFQN());
      ControllerMetaModel controller = ac.get(handle);
      if (controller == null) {
        ac.add(controller = new ControllerMetaModel(handle));
      }
      controller.addMethod(application.model, key, added);
    }
  }

  @Override
  public void processAnnotationRemoved(ApplicationMetaModel metaModel, AnnotationKey key, AnnotationState removed) {
    if (NAMES.contains(key.getType())) {
      ElementHandle.Method methodHandle = (ElementHandle.Method)key.getElement();
      ElementHandle.Class controllerHandle = ElementHandle.Class.create(methodHandle.getFQN());
      ControllersMetaModel controllers = metaModel.getChild(ControllersMetaModel.KEY);
      ControllerMetaModel controller = controllers.get(controllerHandle);
      if (controller != null) {
        controller.remove(methodHandle);
        if (controller.getMethods().isEmpty()) {
          controller.remove();
        }
      }
    }
  }

  @Override
  public void postProcessAnnotations(ApplicationMetaModel application) {
    for (ControllerMetaModel controller : application.getChild(ControllersMetaModel.KEY)) {
      if (controller.modified) {
        controller.modified = false;
        controller.queue(MetaModelEvent.createUpdated(controller));
      }
    }
  }

  @Override
  public void processEvent(ApplicationMetaModel application, MetaModelEvent event) {
    MetaModelObject obj = event.getObject();
    if (obj instanceof ControllerMetaModel) {
      switch (event.getType()) {
        case MetaModelEvent.BEFORE_REMOVE:
          break;
        case MetaModelEvent.UPDATED:
        case MetaModelEvent.AFTER_ADD:
          ControllerMetaModel controller = (ControllerMetaModel)obj;
          written.add(controller);
          break;
      }
    }
  }


  @Override
  public JSON getDescriptor(ApplicationMetaModel application) {
    ControllersMetaModel ac = application.getChild(ControllersMetaModel.KEY);

    // Build routes configuration
    ArrayList<String> controllers = new ArrayList<String>();
    for (ControllerMetaModel controller : ac) {
      controllers.add(controller.getHandle().getFQN() + "_");
    }

    //
    JSON config = new JSON();
    config.set("default", ac.defaultController != null ? ac.defaultController.toString() : null);
    config.set("escapeXML", ac.escapeXML);
    config.map("controllers", controllers);

    //
    return config;
  }

  /** . */
  private static final HashMap<Phase, String> DISPATCH_TYPE = new HashMap<Phase, String>();

  static
  {
    DISPATCH_TYPE.put(Phase.ACTION, Tools.getName(Phase.Action.Dispatch.class));
    DISPATCH_TYPE.put(Phase.VIEW, Tools.getName(Phase.View.Dispatch.class));
    DISPATCH_TYPE.put(Phase.RESOURCE, Tools.getName(Phase.Resource.Dispatch.class));
  }

  @Override
  public void postProcessEvents(ApplicationMetaModel application) {
    // Check everything is OK here
//    for (ControllerMetaModel controller : application.getChild(ControllersMetaModel.KEY)) {
//      for (MethodMetaModel method : controller.getMethods()) {
//        ExecutableElement executableElt = application.model.processingContext.get(method.handle);
//        Iterator<? extends VariableElement> i = executableElt.getParameters().iterator();
//        for (ParameterMetaModel parameter : method.parameters) {
//          VariableElement ve = i.next();
//          if (parameter instanceof InvocationParameterMetaModel) {
//            InvocationParameterMetaModel invocationParameter = (InvocationParameterMetaModel)parameter;
//            TypeElement te = application.model.processingContext.get(invocationParameter.getType());
//            if (!te.toString().equals("java.lang.String") && te.getAnnotation(Mapped.class) == null) {
//              // We should find out who was compiled the bean or the type containing a ref to the class
//              throw ControllerMetaModel.CONTROLLER_METHOD_PARAMETER_NOT_RESOLVED.failure(ve, ve.getSimpleName());
//            }
//          }
//        }
//      }
//    }

    // Emit controllers
    for (Iterator<ControllerMetaModel> i = written.iterator();i.hasNext();) {
      ControllerMetaModel controller = i.next();
      i.remove();
      emitController(application.model.processingContext, controller);
    }
  }

  private void emitController(ProcessingContext env, ControllerMetaModel controller) throws ProcessingException {
    Name fqn = controller.getHandle().getFQN();
    Element origin = env.get(controller.getHandle());
    Collection<MethodMetaModel> methods = controller.getMethods();
    Writer writer = null;
    try {
      JavaFileObject file = env.createSourceFile(fqn + "_", origin);
      writer = file.openWriter();

      //
      writer.append("package ").append(fqn.getParent()).append(";\n");

      // Imports
      writer.append("import ").append(Method.class.getCanonicalName()).append(";\n");
      writer.append("import ").append(Parameter.class.getCanonicalName()).append(";\n");
      writer.append("import ").append(PhaseParameter.class.getCanonicalName()).append(";\n");
      writer.append("import ").append(ContextualParameter.class.getCanonicalName()).append(";\n");
      writer.append("import ").append(Tools.class.getCanonicalName()).append(";\n");
      writer.append("import ").append(Arrays.class.getCanonicalName()).append(";\n");
      writer.append("import ").append(Phase.class.getCanonicalName()).append(";\n");
      writer.append("import ").append(ControllerDescriptor.class.getCanonicalName()).append(";\n");
      writer.append("import ").append(Generated.class.getCanonicalName()).append(";\n");
      writer.append("import ").append(Cardinality.class.getCanonicalName()).append(";\n");
      writer.append("import ").append(Request.class.getCanonicalName()).append(";\n");

      // Open class
      writer.append("@Generated(value={})\n");
      writer.append("public class ").append(fqn.getIdentifier()).append("_ {\n");

      //
      int index = 0;
      for (MethodMetaModel method : methods) {

        //
        String methodRef = "method_" + index++;

        // Method constant
        writer.append("private static final ").append(METHOD_DESCRIPTOR).append("<");
        Tools.nameOf(method.getPhase().getClass(), writer);
        writer.append("> ").append(methodRef).append(" = ");
        writer.append("new ").append(METHOD_DESCRIPTOR).append("<");
        Tools.nameOf(method.getPhase().getClass(), writer);
        writer.append(">(");
        if (method.getId() != null) {
          writer.append("\"").append(method.getId()).append("\",");
        }
        else {
          writer.append("null,");
        }
        writer.append(PHASE).append(".").append(method.getPhase().name()).append(",");
        writer.append(fqn).append(".class").append(",");
        writer.append(TOOLS).append(".safeGetMethod(").append(fqn).append(".class,\"").append(method.getName()).append("\"");
        for (ParameterMetaModel parameter : method.getParameters()) {
          writer.append(",").append(parameter.typeLiteral).append(".class");
        }
        writer.append(')');
        writer.append(", Arrays.<").append(PARAMETER).append(">asList(");
        for (int i = 0;i < method.getParameters().size();i++) {
          ParameterMetaModel parameter = method.getParameters().get(i);
          if (i > 0) {
            writer.append(',');
          }
          if (parameter instanceof PhaseParameterMetaModel) {
            PhaseParameterMetaModel invocationParameter = (PhaseParameterMetaModel)parameter;
            writer.append("new ").
                append(INVOCATION_PARAMETER).append('(').
                append('"').append(parameter.getName()).append('"').append(',').
                append(parameter.typeLiteral).append(".class").append(',').
                append(CARDINALITY).append('.').append(invocationParameter.getCardinality().name()).
                append(')');
          } else {
            writer.append("new ").
                append(CONTEXTUAL_PARAMETER).append('(').
                append('"').append(parameter.getName()).append('"').append(',').
                append(parameter.typeLiteral).append(".class").
                append(')');
          }
        }
        writer.append(')');
        writer.append(");\n");

        //
        String dispatchType = DISPATCH_TYPE.get(method.getPhase());

        // Build list of invocation parameters
        ArrayList<PhaseParameterMetaModel> parameters = new ArrayList<PhaseParameterMetaModel>(method.getParameters().size());
        for (ParameterMetaModel parameter : method.getParameters()) {
          if (parameter instanceof PhaseParameterMetaModel) {
            parameters.add((PhaseParameterMetaModel)parameter);
          }
        }

        // We don't generate dispatch for event phase
        if (method.getPhase() != Phase.EVENT) {
          // Dispatch literal
          writer.append("public static ").append(dispatchType).append(" ").append(method.getName()).append("(");
          for (int i = 0;i < parameters.size();i++) {
            PhaseParameterMetaModel parameter = parameters.get(i);
            if (i > 0) {
              writer.append(',');
            }
            writer.append(parameter.typeLiteral).append(" ").append(parameter.getName());
          }
          writer.append(") { return Request.getCurrent().getContext().create").append(method.getPhase().getClass().getSimpleName()).append("Dispatch(").append(methodRef);
          switch (parameters.size()) {
            case 0:
              break;
            case 1:
              writer.append(",(Object)").append(parameters.get(0).getName());
              break;
            default:
              writer.append(",new Object[]{");
              for (int j = 0;j < parameters.size();j++) {
                if (j > 0) {
                  writer.append(",");
                }
                writer.append(method.getParameter(j).getName());
              }
              writer.append('}');
              break;
          }
          writer.append("); }\n");
        }
      }

      //
      writer.append("public static final ").append(CONTROLLER_DESCRIPTOR).append(" DESCRIPTOR = new ").append(CONTROLLER_DESCRIPTOR).append("(");
      writer.append(fqn.getIdentifier()).append(".class,Arrays.<").append(METHOD_DESCRIPTOR).append("<?>>asList(");
      for (int j = 0;j < methods.size();j++) {
        if (j > 0) {
          writer.append(',');
        }
        writer.append("method_").append(Integer.toString(j));
      }
      writer.append(")");
      writer.append(");\n");

      // Close class
      writer.append("}\n");

      //
      env.log("Generated controller companion " + fqn + "_" + " as " + file.toUri());
    }
    catch (IOException e) {
      throw ControllerMetaModel.CANNOT_WRITE_CONTROLLER_COMPANION.failure(e, origin, controller.getHandle().getFQN());
    }
    finally {
      Tools.safeClose(writer);
    }
  }
}

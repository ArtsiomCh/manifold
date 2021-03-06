/*
 * Copyright (c) 2018 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.api.json;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.script.Bindings;
import javax.script.ScriptException;
import manifold.ext.DataBindings;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.host.IManifoldHost;
import manifold.api.json.schema.IllegalSchemaTypeName;
import manifold.api.json.schema.TypeAttributes;
import manifold.api.type.AbstractSingleFileModel;
import manifold.api.type.ResourceFileTypeManifold;
import manifold.internal.javac.IIssue;
import manifold.internal.javac.SourceJavaFileObject;
import manifold.util.JavacDiagnostic;

/**
 */
class JsonModel extends AbstractSingleFileModel
{
  private IJsonParentType _type;
  private JsonIssueContainer _issues;

  JsonModel( IManifoldHost host, String fqn, Set<IFile> files )
  {
    super( host, fqn, files );
    init();
  }

  private void init()
  {
    _issues = null;
    Bindings bindings;
    try
    {
      bindings = Json.fromJson( ResourceFileTypeManifold.getContent( getFile() ), false, true );
    }
    catch( Exception e )
    {
      Throwable cause = e.getCause();
      if( cause instanceof ScriptException )
      {
        _issues = new JsonIssueContainer( (ScriptException)cause, getFile() );
      }
      bindings = new DataBindings();
    }

    try
    {
      try
      {
        IJsonType type = Json.transformJsonObject( getHost(), getFile().getBaseName(), getFile().toURI().toURL(), null, bindings );
        if( type instanceof IJsonParentType )
        {
          _type = (IJsonParentType)type;
        }
        else
        {
          _type = new JsonStructureType( null, getFile().toURI().toURL(), getFile().getBaseName(), new TypeAttributes() );
        }
      }
      catch( IllegalSchemaTypeName e )
      {
        _type = new ErrantType( getFile().toURI().toURL(), e.getTypeName() );
        if( _issues == null )
        {
          _issues = new JsonIssueContainer( getFile() );
        }
        _issues.addIssues( e );
      }
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
  }

  public IJsonParentType getType()
  {
    return _type;
  }

  @Override
  public void updateFile( IFile file )
  {
    super.updateFile( file );
    init();
  }

  void report( DiagnosticListener<JavaFileObject> errorHandler )
  {
    if( errorHandler == null )
    {
      return;
    }

    List<IIssue> issues = getIssues();
    if( issues.isEmpty() )
    {
      return;
    }

    JavaFileObject file = new SourceJavaFileObject( getFile().toURI() );
    for( IIssue issue : issues )
    {
      Diagnostic.Kind kind = issue.getKind() == IIssue.Kind.Error ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING;
      errorHandler.report( new JavacDiagnostic( file, kind, issue.getStartOffset(), issue.getLine(), issue.getColumn(), issue.getMessage() ) );
    }
  }

  private List<IIssue> getIssues()
  {
    List<IIssue> allIssues = new ArrayList<>();
    if( _issues != null )
    {
      allIssues.addAll( _issues.getIssues() );
    }
    if( _type != null )
    {
      allIssues.addAll( _type.getIssues() );
    }
    return allIssues;
  }
}
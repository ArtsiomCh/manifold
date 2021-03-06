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

import java.util.StringTokenizer;
import javax.script.Bindings;
import manifold.ext.DataBindings;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.host.IModule;
import manifold.api.type.JavaTypeManifold;
import manifold.ext.RuntimeMethods;
import manifold.ext.api.IBindingsBacked;
import manifold.ext.api.Structural;
import manifold.util.ManClassUtil;

/**
 */
public class JsonTypeManifold extends JavaTypeManifold<JsonModel>
{
  @SuppressWarnings("WeakerAccess")
  public static final String FILE_EXTENSION = "json";

  @Override
  public void init( IModule module )
  {
    init( module, (fqn, files) -> new JsonModel( getModule().getHost(), fqn, files ) );
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return fileExtension.equals( FILE_EXTENSION );
  }

  @Override
  public boolean isInnerType( String topLevel, String relativeInner )
  {
    JsonModel model = getModel( topLevel );
    IJsonParentType type = model == null ? null : model.getType();
    if( type == null )
    {
      return false;
    }
    IJsonParentType csr = type;
    for( StringTokenizer tokenizer = new StringTokenizer( relativeInner, "." ); tokenizer.hasMoreTokens(); )
    {
      String childName = tokenizer.nextToken();
      IJsonType child = csr.findChild( childName );
      if( child instanceof IJsonParentType )
      {
        csr = (IJsonParentType)child;
        continue;
      }
      return false;
    }
    return true;
  }

  @Override
  protected String contribute( JavaFileManager.Location location, String topLevelFqn, String existing, JsonModel model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    StringBuilder sb = new StringBuilder();
    sb.append( "package " ).append( ManClassUtil.getPackage( topLevelFqn ) ).append( ";\n\n" )
      .append( "import " ).append( Json.class.getName() ).append( ";\n" )
      .append( "import " ).append( Bindings.class.getName() ).append( ";\n" )
      .append( "import " ).append( DataBindings.class.getName() ).append( ";\n" )
      .append( "import " ).append( IBindingsBacked.class.getName() ).append( ";\n" )
      .append( "import " ).append( Structural.class.getName() ).append( ";\n" )
      .append( "import " ).append( RuntimeMethods.class.getName() ).append( ";\n\n" );
    model.report( errorHandler );
    model.getType().render( sb, 0, true );
    return sb.toString();
  }
}
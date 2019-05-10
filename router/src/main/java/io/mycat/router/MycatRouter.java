/**
 * Copyright (C) <2019>  <chen junwen>
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If
 * not, see <http://www.gnu.org/licenses/>.
 */
package io.mycat.router;

import io.mycat.beans.mycat.MycatSchema;
import io.mycat.router.routeResult.MySQLCommandRouteResultRoute;
import io.mycat.router.routeStrategy.SqlParseRouteRouteStrategy;
import io.mycat.sqlparser.util.BufferSQLContext;
import io.mycat.sqlparser.util.BufferSQLParser;
import java.io.IOException;

/**
 * @author jamie12221
 * @date 2019-05-05 17:04
 **/
public class MycatRouter implements RouteStrategy<RouteContext> {

  final MycatRouterConfig config;
  final BufferSQLParser sqlParser = new BufferSQLParser();
  final BufferSQLContext sqlContext = new BufferSQLContext();
  final RouteContext context;

  public MycatRouter(MycatRouterConfig config) {
    this.config = config;
    context = new RouteContext(config);
  }

  public ResultRoute enterRoute(MycatSchema defaultSchema,String sql) {
    sqlParser.parse(sql.getBytes(), sqlContext);
    return enterRoute(defaultSchema, sqlContext, sql);
  }
  public BufferSQLContext simpleParse(String sql){
    sqlParser.parse(sql.getBytes(),sqlContext);
    return sqlContext;
  }


  public ResultRoute enterRoute(MycatSchema defaultSchema, BufferSQLContext sqlContext, CharSequence sql) {
    this.context.setSqlContext(this.sqlContext);
    int sqlType = this.sqlContext.getSQLType();
    //判断有没有schema
    int schemaCount = this.sqlContext.getSchemaCount();
    if (schemaCount == 0) {
      RouteStrategy routeStrategy = defaultSchema.getRouteStrategy();
      return routeStrategy.route(defaultSchema, sql,    this.context);
    }
    if (schemaCount == 1) {
      String schemaName = this.sqlContext.getSchemaName(0);
      MycatSchema schema = config.getSchemaBySchemaName(schemaName);
      if (schema == null) {
        schema = config.getDefaultSchema();
      }
      RouteStrategy routeStrategy = schema.getRouteStrategy();
      return routeStrategy.route(schema, sql,    this.context);
    } else {

      return this.route(defaultSchema,sql,this.context);
    }
  }

  public MySQLCommandRouteResultRoute enterRoute(String defaultSchemaName, int commandPakcet) {
    MycatSchema defaultSchema = config.getSchemaBySchemaName(defaultSchemaName);
    return enterRoute(defaultSchema, commandPakcet);
  }

  public MySQLCommandRouteResultRoute enterRoute(MycatSchema defaultSchema, int commandPakcet) {
    String defaultDataNode = defaultSchema.getDefaultDataNode();
    MySQLCommandRouteResultRoute result = new MySQLCommandRouteResultRoute();
    result.setCmd(commandPakcet);
    result.setDataNode(defaultDataNode);
    return result;
  }

  public ResultRoute enterRoute(String defaultSchemaName, String sql) {
    MycatSchema defaultSchema = config.getSchemaBySchemaName(defaultSchemaName);
    return enterRoute(defaultSchema, sql);
  }

  public static void main(String[] args) throws CloneNotSupportedException, IOException {
    String root = "D:\\newgit\\f2\\proxy\\src\\main\\resources";
    MycatRouterConfig config = new MycatRouterConfig(root);
    MycatRouter router = new MycatRouter(config);
    router.enterRoute("db1", "select * from travelrecord;");
  }


  @Override
  public ResultRoute route(MycatSchema schema, CharSequence sql, RouteContext routeContext) {
    SqlParseRouteRouteStrategy strategy = routeContext.getSqlParseRouteRouteStrategy();
    strategy.route(schema, sql, context);
    return null;
  }

  public MycatRouterConfig getConfig() {
    return config;
  }
}

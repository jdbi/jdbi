/*
 * Copyright 2004 - 2011 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2;

import java.util.Calendar;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.tweak.BeanMapperFactory;

/*
-- ----------------------------
-- Table structure for `sys_process_deployment`
-- ----------------------------
DROP TABLE IF EXISTS `sys_process_deployment`;
CREATE TABLE `sys_process_deployment` (
  `ID` varchar(128) NOT NULL default '',
  `VERSIONID` varchar(128) default NULL,
  `PROCESSNAME` varchar(128) default NULL,
  `PROCESSGROUPNAME` varchar(64) default NULL,
  `CATEGORYNAME` varchar(64) default NULL,
  `ENGINETYPE` int(1) default '1',
  `PROCESSVER` int(5) default NULL,
  `VERSIONSTATUS` int(1) default NULL,
  `PROCESSADMINISTRATOR` varchar(255) default NULL,
  `CREATETIME` timestamp NULL default NULL,
  `CREATEUSER` varchar(30) default NULL,
  `UPDATETIME` timestamp NULL default NULL,
  `UPDATEUSER` varchar(30) default NULL,
  `HISTORYMAXVERSION` int(6) default NULL,
  `RELEASEUSER` varchar(32) default NULL,
  `RELEASETIME` timestamp NULL default NULL,
  `MESSAGEDIGEST` varchar(255) default NULL,
  PRIMARY KEY  (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of sys_process_deployment
-- ----------------------------
INSERT INTO `sys_process_deployment` VALUES ('_1b8984c80411fea617360ec6ba7160ffccde34d0', '_1b8984c80411fea617360ec6ba7160ffccde34d0', 'TestRule', 'TestCaseGroup', 'test', '0', '1', '0', 'admin', '2012-12-21 10:22:41', 'admin', '2012-12-21 10:22:41', 'admin', '0', null, null, '');
INSERT INTO `sys_process_deployment` VALUES ('_99db376941426bac25928eeb31c84e1d0a98faa6', '_99db376941426bac25928eeb31c84e1d0a98faa6', 'TestCase1', 'TestCaseGroup', 'test', '0', '1', '0', '', '2012-11-13 15:45:30', 'admin', '2012-11-27 09:44:31', '', '3', null, null, '');
INSERT INTO `sys_process_deployment` VALUES ('_cf39a65c62769129850d17d649782a78f68f13bb', '_cf39a65c62769129850d17d649782a78f68f13bb', 'TestCase2', 'TestCaseGroup', 'test', '0', '1', '0', 'admin', '2012-11-13 15:46:14', 'admin', '2012-11-19 17:06:28', 'admin', '6', null, null, '');
INSERT INTO `sys_process_deployment` VALUES ('_e3d7a39faca9f64c9c1d35cd9617bb603b713225', '_e3d7a39faca9f64c9c1d35cd9617bb603b713225', 'TestScriptOne', 'TestCaseGroup', 'test', '0', '1', '0', 'admin', '2012-11-29 09:48:59', 'admin', '2012-11-29 10:31:46', 'admin', '1', null, null, '');

*/
public class TestCalendar {
	DBI dbi;

	@Before
	public void setUp() throws Exception {
		Class.forName("com.mysql.jdbc.Driver");

		dbi = new DBI("jdbc:mysql://127.0.0.1:3306/aws6?characterEncoding=utf-8", "root", "masterkey");

		dbi.registerMapper(new BeanMapperFactory()); //@wjw_note: regidt default Bean Mapper.
	}

	@After
	public void tearDown() throws Exception {
		//
	}

	@Test
	public void testObjUseMapTo() {
		Handle handle = dbi.open();
		try {
			handle.begin();
			try {
				List<SysProcessDeployment> rs = handle.createQuery("select * from sys_process_deployment").mapTo(SysProcessDeployment.class).list();
				for (SysProcessDeployment map : rs) {
					System.out.println(map);
				}
			} catch (Exception e) {
				handle.rollback();
				e.printStackTrace();
			} finally {
				handle.commit();
			}
		} finally {
			handle.close();
		}
	}

	public static class SysProcessDeployment {

		private String categoryname;
		private Calendar createtime;
		private String createuser;
		private int enginetype;
		private int historymaxversion;
		private String id;
		private String messagedigest;
		private String processadministrator;
		private String processgroupname;
		private String processname;
		private int processver;
		private Calendar releasetime;
		private String releaseuser;
		private Calendar updatetime;
		private String updateuser;
		private String versionid;
		private int versionstatus;

		public SysProcessDeployment() {
		}

		public String getCategoryname() {
			return this.categoryname;
		}

		public Calendar getCreatetime() {
			return this.createtime;
		}

		public String getCreateuser() {
			return this.createuser;
		}

		public int getEnginetype() {
			return this.enginetype;
		}

		public int getHistorymaxversion() {
			return this.historymaxversion;
		}

		public String getId() {
			return this.id;
		}

		public String getMessagedigest() {
			return this.messagedigest;
		}

		public String getProcessadministrator() {
			return this.processadministrator;
		}

		public String getProcessgroupname() {
			return this.processgroupname;
		}

		public String getProcessname() {
			return this.processname;
		}

		public int getProcessver() {
			return this.processver;
		}

		public Calendar getReleasetime() {
			return this.releasetime;
		}

		public String getReleaseuser() {
			return this.releaseuser;
		}

		public Calendar getUpdatetime() {
			return this.updatetime;
		}

		public String getUpdateuser() {
			return this.updateuser;
		}

		public String getVersionid() {
			return this.versionid;
		}

		public int getVersionstatus() {
			return this.versionstatus;
		}

		public void setCategoryname(String categoryname) {
			this.categoryname = categoryname;
		}

		public void setCreatetime(Calendar createtime) {
			this.createtime = createtime;
		}

		public void setCreateuser(String createuser) {
			this.createuser = createuser;
		}

		public void setEnginetype(int enginetype) {
			this.enginetype = enginetype;
		}

		public void setHistorymaxversion(int historymaxversion) {
			this.historymaxversion = historymaxversion;
		}

		public void setId(String id) {
			this.id = id;
		}

		public void setMessagedigest(String messagedigest) {
			this.messagedigest = messagedigest;
		}

		public void setProcessadministrator(String processadministrator) {
			this.processadministrator = processadministrator;
		}

		public void setProcessgroupname(String processgroupname) {
			this.processgroupname = processgroupname;
		}

		public void setProcessname(String processname) {
			this.processname = processname;
		}

		public void setProcessver(int processver) {
			this.processver = processver;
		}

		public void setReleasetime(Calendar releasetime) {
			this.releasetime = releasetime;
		}

		public void setReleaseuser(String releaseuser) {
			this.releaseuser = releaseuser;
		}

		public void setUpdatetime(Calendar updatetime) {
			this.updatetime = updatetime;
		}

		public void setUpdateuser(String updateuser) {
			this.updateuser = updateuser;
		}

		public void setVersionid(String versionid) {
			this.versionid = versionid;
		}

		public void setVersionstatus(int versionstatus) {
			this.versionstatus = versionstatus;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((categoryname == null) ? 0 : categoryname.hashCode());
			result = prime * result + ((createtime == null) ? 0 : createtime.hashCode());
			result = prime * result + ((createuser == null) ? 0 : createuser.hashCode());
			result = prime * result + enginetype;
			result = prime * result + historymaxversion;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result + ((messagedigest == null) ? 0 : messagedigest.hashCode());
			result = prime * result + ((processadministrator == null) ? 0 : processadministrator.hashCode());
			result = prime * result + ((processgroupname == null) ? 0 : processgroupname.hashCode());
			result = prime * result + ((processname == null) ? 0 : processname.hashCode());
			result = prime * result + processver;
			result = prime * result + ((releasetime == null) ? 0 : releasetime.hashCode());
			result = prime * result + ((releaseuser == null) ? 0 : releaseuser.hashCode());
			result = prime * result + ((updatetime == null) ? 0 : updatetime.hashCode());
			result = prime * result + ((updateuser == null) ? 0 : updateuser.hashCode());
			result = prime * result + ((versionid == null) ? 0 : versionid.hashCode());
			result = prime * result + versionstatus;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SysProcessDeployment other = (SysProcessDeployment) obj;
			if (categoryname == null) {
				if (other.categoryname != null)
					return false;
			} else if (!categoryname.equals(other.categoryname))
				return false;
			if (createtime == null) {
				if (other.createtime != null)
					return false;
			} else if (!createtime.equals(other.createtime))
				return false;
			if (createuser == null) {
				if (other.createuser != null)
					return false;
			} else if (!createuser.equals(other.createuser))
				return false;
			if (enginetype != other.enginetype)
				return false;
			if (historymaxversion != other.historymaxversion)
				return false;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			if (messagedigest == null) {
				if (other.messagedigest != null)
					return false;
			} else if (!messagedigest.equals(other.messagedigest))
				return false;
			if (processadministrator == null) {
				if (other.processadministrator != null)
					return false;
			} else if (!processadministrator.equals(other.processadministrator))
				return false;
			if (processgroupname == null) {
				if (other.processgroupname != null)
					return false;
			} else if (!processgroupname.equals(other.processgroupname))
				return false;
			if (processname == null) {
				if (other.processname != null)
					return false;
			} else if (!processname.equals(other.processname))
				return false;
			if (processver != other.processver)
				return false;
			if (releasetime == null) {
				if (other.releasetime != null)
					return false;
			} else if (!releasetime.equals(other.releasetime))
				return false;
			if (releaseuser == null) {
				if (other.releaseuser != null)
					return false;
			} else if (!releaseuser.equals(other.releaseuser))
				return false;
			if (updatetime == null) {
				if (other.updatetime != null)
					return false;
			} else if (!updatetime.equals(other.updatetime))
				return false;
			if (updateuser == null) {
				if (other.updateuser != null)
					return false;
			} else if (!updateuser.equals(other.updateuser))
				return false;
			if (versionid == null) {
				if (other.versionid != null)
					return false;
			} else if (!versionid.equals(other.versionid))
				return false;
			if (versionstatus != other.versionstatus)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "SysProcessDeployment [categoryname=" + categoryname + ", createtime=" + createtime + ", createuser=" + createuser + ", enginetype=" + enginetype + ", historymaxversion=" + historymaxversion + ", id=" + id + ", messagedigest=" + messagedigest + ", processadministrator=" + processadministrator + ", processgroupname=" + processgroupname + ", processname=" + processname + ", processver=" + processver + ", releasetime=" + releasetime + ", releaseuser=" + releaseuser + ", updatetime=" + updatetime + ", updateuser=" + updateuser + ", versionid=" + versionid + ", versionstatus=" + versionstatus + "]";
		}

	}

}

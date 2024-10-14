package net.yadaframework.example.web.dashboard;

import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import net.yadaframework.example.persistence.entity.UserProfile;
import net.yadaframework.persistence.YadaDataTableDao;
import net.yadaframework.persistence.YadaSql;
import net.yadaframework.web.YadaDatatablesRequest;

@Controller
@RequestMapping("/dashboard/user")
public class UserProfileController {
	
	@Autowired private YadaDataTableDao yadaDataTableDao;
	
	@RequestMapping("")
	public String users() {
		return "/dashboard/users";
	}
	
	@RequestMapping("/legacy")
	public String legacy() {
		return "/dashboard/usersLegacy";
	}
	
	@RequestMapping(value ="/userProfileTablePage", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody public Map<String, Object> userProfileTablePage(YadaDatatablesRequest yadaDatatablesRequest, Locale locale) {
//		boolean usersSuspended = yadaDatatablesRequest.getExtraParam().get("usersSuspendedSet")!=null;
		YadaSql yadaSql = yadaDatatablesRequest.getYadaSql();
//		yadaSql.where(usersSuspended,":suspendedRole MEMBER OF e.userCredentials.roles") //.and()
//				.where(!usersSuspended,":suspendedRole NOT MEMBER OF e.userCredentials.roles") //.and()
//				.setParameter("suspendedRole", VlbUtil.ROLE_SUSPENDED_ID);

		// yadaDatatablesRequest.addExtraJsonAttribute("enabled");
		// yadaDatatablesRequest.addExtraJsonAttribute("registrationDate");
		// yadaDatatablesRequest.addExtraJsonAttribute("email");
		// yadaDatatablesRequest.addExtraJsonAttribute("loginDate");
		Map<String, Object> result = yadaDataTableDao.getConvertedJsonPage(yadaDatatablesRequest, UserProfile.class, locale);
		return result;
	}
	

}

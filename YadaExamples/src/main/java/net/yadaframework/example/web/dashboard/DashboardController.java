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
@RequestMapping("/dashboard")
public class DashboardController {
	
	@Autowired private YadaDataTableDao yadaDataTableDao;
	
	@RequestMapping("")
	public String dashboard() {
		return "/dashboard/dashboard";
	}


}

document.addEventListener('DOMContentLoaded', function () {
  fetchEmployees();
});

function fetchEmployees() {
  fetch('/employees/active')
    .then((response) => response.json())
    .then((data) => populateTable(data))
    .catch((error) => console.error('Error fetching employees:', error));
}

function populateTable(employees) {
  const employeeBody = document.getElementById('employeeBody');
  employeeBody.innerHTML = ''; // Clear existing rows

  employees.forEach((employee) => {
    const row = document.createElement('tr');
    row.innerHTML = `
          <td>${employee.id}</td>
          <td>${employee.name}</td>
          <td>${employee.position}</td>
          <td>${employee.active ? 'Yes' : 'No'}</td>
      `;
    employeeBody.appendChild(row);
  });
}

function filterEmployees() {
  const searchInput = document.getElementById('search').value.toLowerCase();
  const rows = document.querySelectorAll('#employeeBody tr');

  rows.forEach((row) => {
    const name = row.cells[1].textContent.toLowerCase();
    if (name.includes(searchInput)) {
      row.style.display = '';
    } else {
      row.style.display = 'none';
    }
  });
}

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddAdminGameweekComponent } from './add-admin-gameweek.component';

describe('AddAdminGameweekComponent', () => {
  let component: AddAdminGameweekComponent;
  let fixture: ComponentFixture<AddAdminGameweekComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AddAdminGameweekComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AddAdminGameweekComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
